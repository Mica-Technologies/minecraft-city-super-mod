#!/usr/bin/env node
/*
 * City Super Mod — incremental language file translation
 *
 * Reads src/main/resources/assets/csm/lang/en_us.lang as the English source of truth and
 * refreshes es_es.lang / de_de.lang / sv_se.lang beside it. By default only keys missing from
 * a target file are translated, so adding one block costs one API call per language rather
 * than a full sweep.
 *
 * Usage:
 *   npm install                 # one-time, fetches google-translate-api-x
 *   npm run translate           # incremental — only keys missing from each target
 *   npm run translate:dry       # report what would change, write nothing
 *   npm run translate:force     # re-translate every key, overwriting existing values
 *   node translate-lang.js --only=sv_se       # restrict to one locale
 *
 * This is deliberately NOT the tool that produced the current translations. The bulk pass was
 * done by language models working against a glossary, because a general-purpose translator has
 * no memory between calls and this file is full of vocabulary where that matters: "Sign" is the
 * head noun of 483 entries, "Solid" means the round ball indication rather than a steady one,
 * "Border" is a signal backplate, and "Doghouse" is a signal head in one block and a literal
 * kennel in another. This tool exists to keep the files current as names are added, at a volume
 * where the free endpoint is appropriate.
 *
 * Differences from the launcher's tools/i18n/translate-locales.js, which this mirrors:
 *   - Minecraft .lang files are read as raw UTF-8, so values are written directly. The launcher
 *     escapes non-ASCII as \uXXXX because java.util.Properties needs it; doing that here would
 *     put a literal backslash-u in the player's inventory.
 *   - Line endings follow en_us.lang (CRLF in this repo) so a refresh does not rewrite the file.
 *   - The sentinel mechanism protects manufacturer names and acronyms as well as %s specifiers.
 *   - Glossary terms are substituted rather than translated, so incremental additions match the
 *     vocabulary the bulk pass established.
 */

import { translate } from 'google-translate-api-x';
import { readFile, writeFile, access } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { dirname, join, resolve } from 'node:path';

const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(SCRIPT_DIR, '..', '..');
const LANG_DIR = join(REPO_ROOT, 'src', 'main', 'resources', 'assets', 'csm', 'lang');
const SOURCE_FILE = join(LANG_DIR, 'en_us.lang');
const GLOSSARY_FILE = join(SCRIPT_DIR, 'glossary.json');

const TARGET_LOCALES = [
    { tag: 'es_es', googleCode: 'es', name: 'Spanish' },
    { tag: 'de_de', googleCode: 'de', name: 'German' },
    { tag: 'sv_se', googleCode: 'sv', name: 'Swedish' },
];

const FORCE = process.argv.includes('--force');
const DRY_RUN = process.argv.includes('--dry-run');
const ONLY = (process.argv.find((a) => a.startsWith('--only=')) || '').split('=')[1];

// 250ms is what the launcher settled on: 120ms drew occasional "Partial Translation Request
// Fail" rate-limit responses from the free endpoint.
const DELAY_MS = 250;

/* ---------------------------------------------------------------------------------------- */
/* .lang I/O                                                                                  */
/* ---------------------------------------------------------------------------------------- */

/** Parse a .lang file. Comments (#) and blank lines are preserved only for the source file's
 *  ordering; targets are always rewritten in the source's key order. */
function parseLang(text) {
    const values = {};
    const keysInOrder = [];
    for (const line of text.split(/\r?\n/)) {
        const trimmed = line.trim();
        if (trimmed === '' || trimmed.startsWith('#')) continue;
        const eq = line.indexOf('=');
        if (eq < 0) continue;
        const key = line.substring(0, eq);
        if (!(key in values)) keysInOrder.push(key);
        // Values are NOT trimmed: several timing strings begin with meaningful spaces.
        values[key] = line.substring(eq + 1);
    }
    return { keysInOrder, values };
}

function formatLang(keysInOrder, values, newline) {
    return keysInOrder
        .filter((k) => values[k] !== undefined)
        .map((k) => `${k}=${values[k]}`)
        .join(newline) + newline;
}

async function fileExists(path) {
    try { await access(path); return true; } catch { return false; }
}

/* ---------------------------------------------------------------------------------------- */
/* Protection                                                                                 */
/* ---------------------------------------------------------------------------------------- */

/** Build one regex matching every protected term, longest first so "American Electric" wins
 *  over "GE" inside it. Word boundaries keep "GE" from matching inside "GENESIS". */
export function buildProtectedPattern(terms) {
    const escaped = [...terms]
        .sort((a, b) => b.length - a.length)
        .map((t) => t.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'));
    return new RegExp(`(?<![A-Za-z0-9])(${escaped.join('|')})(?![A-Za-z0-9])`, 'g');
}

/** Swap %s specifiers and protected terms for ASCII sentinels. The sentinels are plain tokens
 *  that survive a round trip through a translator without being reworded or reordered. */
export function protectValue(text, protectedPattern) {
    const captured = [];
    const stash = (match) => {
        captured.push(match);
        return `__CSM${captured.length - 1}__`;
    };
    let out = text.replace(/%[sd]/g, stash);
    out = out.replace(protectedPattern, stash);
    return { protectedText: out, captured };
}

export function restoreValue(translated, captured) {
    let restored = translated;
    for (let i = 0; i < captured.length; i++) {
        // Case-insensitive: some target languages lowercase all-caps tokens.
        restored = restored.replace(new RegExp(`__\\s*CSM${i}\\s*__`, 'gi'), captured[i]);
    }
    return restored;
}

/** Apply the pinned glossary to a translated value, so recurring vocabulary matches the bulk
 *  pass. Returns the value plus the list of terms whose expected translation was missing. */
function applyGlossary(english, translated, terms) {
    const drifted = [];
    for (const [en, target] of Object.entries(terms)) {
        const present = new RegExp(`(?<![A-Za-z0-9])${en.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}(?![A-Za-z0-9])`, 'i');
        if (present.test(english) && !translated.includes(target)) {
            drifted.push(`${en} -> expected "${target}"`);
        }
    }
    return drifted;
}

/* ---------------------------------------------------------------------------------------- */

async function translateValue(text, targetCode, protectedPattern) {
    if (text.trim() === '') return text;
    const { protectedText, captured } = protectValue(text, protectedPattern);
    // A value that is nothing but protected tokens has no translatable content left.
    if (protectedText.replace(/__CSM\d+__/g, '').trim() === '') return text;
    const result = await translate(protectedText, { from: 'en', to: targetCode });
    return restoreValue(result.text, captured);
}

async function main() {
    const glossary = JSON.parse(await readFile(GLOSSARY_FILE, 'utf8'));
    const protectedPattern = buildProtectedPattern(glossary.protected);

    const sourceRaw = await readFile(SOURCE_FILE, 'utf8');
    const newline = sourceRaw.includes('\r\n') ? '\r\n' : '\n';
    const source = parseLang(sourceRaw);
    console.log(`Source ${SOURCE_FILE}`);
    console.log(`  ${source.keysInOrder.length} keys, ${newline === '\r\n' ? 'CRLF' : 'LF'} endings`);

    let totalCalls = 0, totalKept = 0, totalFailed = 0;

    for (const locale of TARGET_LOCALES) {
        if (ONLY && ONLY !== locale.tag) continue;
        const targetPath = join(LANG_DIR, `${locale.tag}.lang`);
        const existing = (await fileExists(targetPath))
            ? parseLang(await readFile(targetPath, 'utf8'))
            : { keysInOrder: [], values: {} };

        const terms = glossary.terms[locale.tag] || {};
        const merged = {};
        let translated = 0, kept = 0, failed = 0;
        const drift = [];

        console.log(`\n-> ${locale.name} (${locale.tag})  [existing: ${existing.keysInOrder.length} keys]`);

        for (const key of source.keysInOrder) {
            const english = source.values[key];
            const current = existing.values[key];

            if (!FORCE && current !== undefined && current !== '') {
                merged[key] = current;
                kept++; totalKept++;
                continue;
            }
            if (DRY_RUN) {
                console.log(`  [dry] ${key} :: "${english.slice(0, 60)}"`);
                merged[key] = current !== undefined ? current : english;
                continue;
            }
            try {
                const result = await translateValue(english, locale.googleCode, protectedPattern);
                merged[key] = result;
                const missing = applyGlossary(english, result, terms);
                if (missing.length) drift.push(`${key}: ${missing.join(', ')}`);
                translated++; totalCalls++;
            } catch (err) {
                console.warn(`  x ${key} :: ${err.message || err}`);
                merged[key] = english;   // fall back to English so the file still loads
                failed++; totalFailed++;
            }
            await new Promise((res) => setTimeout(res, DELAY_MS));
        }

        // Keys that exist only in the target (a name removed from English) are dropped, so the
        // file cannot accumulate orphans.
        const orphans = existing.keysInOrder.filter((k) => !(k in source.values));
        if (orphans.length) console.log(`  dropping ${orphans.length} key(s) no longer in English`);

        if (!DRY_RUN) {
            await writeFile(targetPath, formatLang(source.keysInOrder, merged, newline), 'utf8');
        }
        console.log(`  done: ${translated} translated, ${kept} kept, ${failed} failed`);
        if (drift.length) {
            console.log(`  ${drift.length} value(s) did not come back with the pinned glossary term:`);
            for (const d of drift.slice(0, 15)) console.log(`     ${d}`);
            if (drift.length > 15) console.log(`     ... and ${drift.length - 15} more`);
            console.log('  Review these by hand — the machine translation ignored settled vocabulary.');
        }
    }

    console.log(`\nTotals: ${totalCalls} API calls, ${totalKept} kept, ${totalFailed} failed`);
    if (DRY_RUN) console.log('(dry run — nothing written)');
    console.log('\nRun the validator over anything this wrote:');
    console.log('  python dev-env-utils/scripts/validate_lang_translations.py \\');
    console.log('      src/main/resources/assets/csm/lang/en_us.lang <target>.lang');
}

// Only run when invoked directly. The helpers above are exported for testing, and an
// import must not kick off a translation run as a side effect.
const invokedDirectly = process.argv[1]
    && resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (invokedDirectly) {
    main().catch((err) => {
        console.error(err);
        process.exit(1);
    });
}
