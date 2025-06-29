plugins {
    id("com.falsepattern.fpgradle-mc") version "0.19.2"
}

group = "mega"

minecraft_fp {
    mod {
        modid = "visualprospecting"
        name = "VisualProspecting"
        rootPkg = "com.sinthoras.visualprospecting"
    }

    mixin {
        pkg = "mixins"
        pluginClass = "mixinplugin.MixinPlugin"
    }

    core {
        accessTransformerFile = "visualprospecting_at.cfg"
    }

    tokens {
        tokenClass = "Tags"
    }

    publish {
        maven {
            repoUrl = "https://mvn.falsepattern.com/gtmega_releases/"
            repoName = "mega"
        }
    }
}

repositories {
    exclusive(mega(), "mega", "gtmega", "codechicken")
    cursemavenEX()
    ic2EX()
    exclusive(horizon(), "com.github.GTNewHorizons")
    exclusive(maven("ursv", "https://mvn.falsepattern.com/usrv/"), "eu.usrv")
}

dependencies {
    shadowImplementation("com.github.GTNewHorizons:Enklume:2.0.0:dev")

    implementation(ic2)
    implementation("gtmega:gt5u-mc1.7.10:5.44.10-mega:dev")

    implementation("codechicken:notenoughitems-mc1.7.10:2.7.51-mega:dev")
    implementation("codechicken:codechickencore-mc1.7.10:1.4.2-mega:dev")
    compileOnly("com.github.GTNewHorizons:TCNodeTracker:1.1.6:dev") {
        excludeDeps()
    }

    compileOnly(deobfCurse("journeymap-32274:2367915"))
    compileOnly(deobfCurse("xaeros-minimap-263420:6012805"))
    compileOnly(deobfCurse("xaerosworldmap-317780:5987124"))
    compileOnly(deobfCurse("voxelmap-225179:2462146"))
    compileOnly(deobfCurse("thaumcraft-223628:2227552"))
}
