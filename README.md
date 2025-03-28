# ModShield
A lightweight simple to use mod, allowing server admins to check client mods as well as forbid players from using certain mods.
Or allow players to use only specific mods.

# Usage
Simply download the latest mod version on the server and the client.
It's important to note that a server running ModShield will require all clients to use it as well.

# Server Config
All configuration is done through the config file: `config/modshield.properties`.
After you launch the mod for the first time a template version will be automatically generated, with descriptions of all options and how to use them.

<details>
<summary>Show Example</summary>

```properties
#This is a template ModShield config file.
#If you want to allow only certain mods add them in the disallowed option, separated by commas.
#This way only mods in that option can be used by the client.
#
#If you want to disallow mods put them int the allowed option, separated by commas.
#
#savePlayerMods - when set to true ModShield will save mods used by players, that can be accessed through the API.
#onlyAllowServerMods - when set to true only mods found on the server will be allowed on the client.
#alwaysAllowedPlayers - list of players that can run every mod, separated by commas, UUIDs or names
#Sun Mar 23 00:29:03 CET 2025
allowed=
disallowed=
onlyAllowServerMods=false
savePlayerMods=false
```
</details>

# API Usage (For Mod Developers)
To use the mod as a library add the maven repository and declare it as a dependency, in your `build.gradle` file like so:
```groovy
// Add the repository
repositories {
    maven {
        url = 'https://maven.avarisgates.com' // ModShield
    }
}

// Declare it as a dependency
dependencies {
    // To package the mod with yours
    modImplementation "com.avaris:ModShield:${project.modshield_version}"
    
    // To use the mod in a seperate .jar file
    // This won't strictly require the mod
    compileOnly "com.avaris:ModShield:${project.modshield_version}"
}
```
It's also recommended to add the following to your `gradle.properties` file:
```properties
mod_shield=CURRENT_VERSION
```
Be sure to add replace `CURRENT_VERSION` with the version you wish to use.
It's always recommended to use the latest one.