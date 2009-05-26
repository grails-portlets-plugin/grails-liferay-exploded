/** 
 * This file contains callbacks that handle grails events for this plugin.
 */


//
// define exploded war file directories
//
def explodedDir = "${basedir}/target/exploded/${grailsAppName}"




/**
 * Deletes the exploded war directory we generate.
 */
def wipeExplodedWarDirectory(String explodedDir)
{
      // delete the 'exploded' directory
      ant.delete(dir: explodedDir)
}

//======================================================================
//      Event callbacks below...
//
//======================================================================

/**
 * Handle 'grails clean' event for this plugin. Removes this plugin's artifacts.
 */
eventCleanStart = {
    wipeExplodedWarDirectory(explodedDir)
}





