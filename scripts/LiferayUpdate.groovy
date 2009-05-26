

// requires a fresh compile before starting
includeTargets << grailsScript("Compile")


//
// define exploded war file directories
//
def explodedDir = "${basedir}/target/exploded/${grailsAppName}"

def webInfDir = "${explodedDir}/WEB-INF"





/**
 * Copies i18n resource files that changed to exploded directory.
 */
def copyI18nFiles(String webInfDir, String srcDir)
{
        String source = "${srcDir}/grails-app/i18n"
        String destination = "${webInfDir}/grails-app/i18n"

        ant.copy(todir : destination) {
                fileset(dir : source)
        }
}

/**
 * Copies view files that changed to exploded directory.
 */
def copyViewFiles(String webInfDir, String srcDir)
{
        String source = "${srcDir}/grails-app/views"
        String destination = "${webInfDir}/grails-app/views"

        ant.copy(todir : destination) {
                fileset(dir : source)
        }
}


/**
 * Copies class files that changed to exploded directory.
 */
def copyClassFiles(String webInfDir, String classesDir)
{
        String source = classesDir
        String destination = "${webInfDir}/classes"

        ant.copy(todir : destination) {
                fileset(dir : source)
        }
}


/*
 * Touches a known class file to force Tomcat to update itself.  If user
 * modified a view or other non-class file, Tomcat won't reload the portlet without
 * doing this.
 */
def touchKnownClass(String webInfDir)
{
        String classFile = "${webInfDir}/classes/LiferayGrailsPlugin.class"

        ant.touch(file : classFile)
}


target(main: "Updates exploded war directory with fresh artifacts") {

   // Requires 'grails compile' first.
   depends(parseArguments, compile)

   copyViewFiles(webInfDir, basedir)
   copyI18nFiles(webInfDir, basedir)
   copyClassFiles(webInfDir, classesDir.toString())

   touchKnownClass(webInfDir)


}

setDefaultTarget(main)
