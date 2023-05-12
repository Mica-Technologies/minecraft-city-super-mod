# City Super Mod
The City Super Mod is a mod for Minecraft 1.12.2 that brings a number of blocks and items to help build your Minecraft city. Initially, the mod was built for the City of MCLA (now City of Alto), but will soon become publicly available as source-code and a compiled mod.

## Information and Documentation
Information and documentation for the City Super Mod is available on the [City Super Mod Wiki](https://github.com/Mica-Technologies/minecraft-city-super-mod/wiki). This documentation is currently under development, and at this time, may be incomplete or incorrect. 

## Issues
Having an issue with the mod? Error in the documentation or it just isn't clear enough? Let us know by using the [GitHub issue tracker](https://github.com/Mica-Technologies/minecraft-city-super-mod/issues).

## Team
Contributions and modifications are not limited to the development team, and it is encouraged to file issues and create pull requests.

<img src="https://minotar.net/armor/bust/HawkA97/100.png" width="50"/>

**Name:** Alex<br/>
**GitHub Username:** mica-alex<br/>
**Minecraft Username:** HawkA97


<img src="https://minotar.net/armor/bust/AngelWingsPanda/100.png" width="50"/>

**Name:** Brandon<br />
**GitHub Username:** AngelWingsPanda<br />
**Minecraft Username:** AngelWingsPanda

## Development
### IDE/Making Changes
The preferred development environment/IDE for the City Super Mod is [IntelliJ IDEA](https://www.jetbrains.com/idea/download). 
After opening IntelliJ, choose the option "Get from Version Control", which allows you to download and open an IntelliJ project from a Git server.

<img src="assets/images-readme/getfromvctl.png" width="200" alt="Get from Version Control Button Image"/>

#### Adding a Block
To add a block, you'll need to do the following:
 
- Create a new block class in `src/main/java/com/micatechnologies/minecraft/csm/block`. 
- Create a block model file in `src/main/resources/assets/csm/models/block`.
- Create a block item model file in `src/main/resources/assets/csm/models/item`.
- Create a block state file in `src/main/resources/assets/csm/blockstates`.
- Add block name mapping to `src/main/resources/assets/csm/lang/en_us.lang`

If your block model does not use a built-in Minecraft block model base, add the custom block model to `src/main/resources/assets/csm/models/custom`.

##### Adding a Model
To maintain proper segregation of model components, there are three models folders, `blocks`, `custom` and `items
`. To add a new model, simply place the complete model in to the `custom` folder. In the `blocks` and `items` folders
, add a model with the proper name and populate it with the following content:

```json
{
  "parent": "csm:custom/[name of model in custom]"
}
``` 

### Contributing Code/Changes
To contribute code, you will need to push your modifications to GitHub on a new branch. 
To protect the working code, modification of the `master` branch is not permitted except through pull request. 
After you submit a pull request, the development team will need to review and approve your modifications/contributions before they can be merged.

To learn more about using Git integration with the IntelliJ IDEA IDE, please see [https://www.jetbrains.com/help/idea/using-git-integration.html](https://www.jetbrains.com/help/idea/using-git-integration.html).

To learn more about the version control system Git, please see [https://git-scm.com/doc](https://git-scm.com/doc).

## Credits
Development and code for the City Super Mod began with [Pylo MCreator](https://mcreator.net), a decent Minecraft mod maker for those who don't know how to code that was formally closed source and is now maintained by the community. 
Current development is performed standalone with the JetBrains IntelliJ IDEA IDE.


