import groovy.xml.StreamingMarkupBuilder
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

includeTargets << grailsScript("Init")
includeTargets << grailsScript("War")

//
// define exploded war file directories
//
def explodedDir = "${basedir}/target/exploded/${grailsAppName}"
def metaInfDir = "${explodedDir}/META-INF"
def webInfDir = "${explodedDir}/WEB-INF"

// This is where local Lifray is installed
String liferayHome = ant.project.properties."environment.LIFERAY_HOME"




/**
 * Finds the given plugin's configration file by looking for it in user's work directory.
 */
private File findPluginConfigFile(String pluginName, String pluginConfigFile) {

    String pluginDirectoryPattern = "file:${pluginsHome}/${pluginName}-*/${pluginConfigFile}"

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver()

    try {
        Resource[] matches = resolver.getResources(pluginDirectoryPattern)
        if (matches) {
            Resource resource = matches[0]
            return (resource?.file)
        }

    } catch (IllegalArgumentException e) { /* eatme */ }

    return null
}

/**
 * Finds the given plugin's installed directory.
 */
private String getPluginDir(String pluginName, String pluginConfigFile) {

    File f = findPluginConfigFile(pluginName, pluginConfigFile)
    if (f == null) {
        // Bomb out if plugin wasn't installed correctly.
        println "Fatal Error! Plugin file ${pluginConfigFile} not found. Make sure plugin installed correctly."
        System.exit(-1)
    }

    return (f.toString() - "/${pluginConfigFile}")
}

/**
 * Creates a 'context.xml' file for the portlet in /META-INF directory.
 */
def createLiferayContextFile(String metaInfDir) {
    def xmlWriter = new StreamingMarkupBuilder()

    def xml = xmlWriter.bind {
        'Context'(antiJARLocking: "true", antiResourceLookup: "true")
    }

    String filename = "${metaInfDir}/context.xml"

    def xmlFile = new File(filename)

    if (xmlFile.exists()) xmlFile.delete()
    String reformatted = xml.toString().replace('\'', '"')

    xmlFile.write(reformatted)
}


/**
 * Copies Liferay's tag library files under the /META-INF/tld directory to exploded war.
 */
def copyTagLibraryFiles(String pluginDir, String webInfDir) {
    copyFiles(pluginDir, webInfDir, 'tld')
}



/**
 * Copies Liferay's /WEB-INF/lib jar files to exploded war.
 */
def copyLibFiles(String pluginDir, String webInfDir) {
    copyFiles(pluginDir, webInfDir, 'lib')
}


/**
 * Copies Liferay's /WEB-INF/classes properties files into exploded war.
 */
def copyPropertiesFiles(String pluginDir, String webInfDir) {
    copyFiles(pluginDir, webInfDir, 'classes')
}


/**
 * Copies all files from plugin's directory to exploded war WEB-INF directory.
 * This method presumes all files in the plugin's "grails-app/liferay-files/WEB-INF/" directory
 * are to be copied to the exploded war without modification.
 */
def copyFiles(String pluginDir, String webInfDir, String subdir) {
    String source = "${pluginDir}/grails-app/liferay-files/WEB-INF/${subdir}"
    String destination = webInfDir + "/${subdir}"

    ant.copy(todir: destination) {
        fileset(dir: source)
    }
}

/**
 * Generates liferay-specific artifact files for portlet deployment.
 */
def generateLiferayArtifacts(String metaInfDir, String webInfDir) {

    final String pluginName = 'liferay'           // Name of our plugin.
    final String pluginConfigFile = 'plugin.xml'   // Our plugin's deployed config file.

    final String pluginDir = getPluginDir(pluginName, pluginConfigFile)

    createLiferayContextFile(metaInfDir)
    copyTagLibraryFiles(pluginDir, webInfDir)
    copyLibFiles(pluginDir, webInfDir)
    copyPropertiesFiles(pluginDir, webInfDir)
}


/**
 * Deletes the exploded war directory we generate.
 */
def wipeExplodedWarDirectory(String explodedDir) {

    // delete the 'exploded' directory
    ant.delete(dir: explodedDir)
}

//--------------------------------------------------------------------
// WEB-INF file handling code below
//--------------------------------------------------------------------


/**
 * Finds all portlet names in /grails-app directory. Note it strips off the trailing "Portlet.groovy" fle suffix.
 */
private String[] findPortlets() {

    def portletFiles = [
            'file:./grails-app/portlets/**/*Portlet.groovy',
            'file:./plugins/*/grails-app/portlets/**/*Portlet.groovy'
    ]

    def portlets = []

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver()

    portletFiles.each {pattern ->
        Resource[] files
        try {
            files = resolver.getResources(pattern)
            files?.each {resource ->
                portlets << (resource.filename - 'Portlet.groovy')
            }
        } catch (IllegalArgumentException e) {
            // Note: eat this exception. it means path was not found.
        }
    }


    return portlets
}


/**
 * Returns an XmlSlurper parsed Xml file.
 */
private def readWebXmlFile(String filename) {

    // read portlet plugin modified web.xml file
    def portletWebXml = new XmlSlurper().parse(new File(filename))

    return portletWebXml
}

/**
 * Appends new <taglib> entries to web.xml file for Liferay.
 */
private def fixTagLibTags(def portletWebXml) {

    def taglibs = portletWebXml.'jsp-config'.'taglib'

    // The last <taglib> entry in our file.
    def lastTagLib = taglibs[taglibs.size() - 1]

    //
    // Appends Liferay-specific taglib to web.xml
    //
    lastTagLib + {
        taglib {
            'taglib-uri'('http://java.sun.com/portlet_2_0')
            'taglib-location'('/WEB-INF/tld/liferay-portlet.tld')
        }
        taglib {
            'taglib-uri'('http://liferay.com/tld/portlet')
            'taglib-location'('/WEB-INF/tld/liferay-portlet-ext.tld')
        }
        taglib {
            'taglib-uri'('http://liferay.com/tld/security')
            'taglib-location'('/WEB-INF/tld/liferay-security.tld')
        }
        taglib {
            'taglib-uri'('http://liferay.com/tld/theme')
            'taglib-location'('/WEB-INF/tld/liferay-theme.tld')
        }
        taglib {
            'taglib-uri'('http://liferay.com/tld/ui')
            'taglib-location'('/WEB-INF/tld/liferay-ui.tld')
        }
        taglib {
            'taglib-uri'('http://liferay.com/tld/util')
            'taglib-location'('/WEB-INF/tld/liferay-util.tld')
        }
    }


}

/**
 * Removes pluto-specific artifacs from web.xml document.
 */
private def removePlutoArtifacts(def portletWebXml, String portletName) {

    //
    // Remove Pluto's servlet mapping for our portlet URL
    //
    def servletUrlPattern =
    portletWebXml.'servlet-mapping'.'url-pattern'.find {
        it == "/PlutoInvoker/${portletName}"
    }
    servletUrlPattern?.replaceNode {node ->
        'url-pattern'("/${portletName}/*")
    }

    //
    // Fix pluto's servlet class mapping for Liferay
    //
    def servletElement = portletWebXml.'servlet'.find {
        it.'servlet-name' == portletName;
    }
    servletElement?.'servlet-class' = 'com.liferay.portal.kernel.servlet.PortletServlet'
    servletElement?.'load-on-startup' = 0
    servletElement?.'init-param'.'param-name' = 'portlet-class'
    servletElement?.'init-param'.'param-value' = 'org.codehaus.grails.portlets.GrailsDispatcherPortlet'

    //
    // Remove Pluto's extra gsp servlet parameters
    //
    def gspServlet = portletWebXml.'servlet'.find {
        it.'servlet-name' == 'gsp'
    }
    // Note: set node to an empty closure to remove the element and it's children
    gspServlet?.'init-param'.replaceNode({})

}


/**
 *  Removes the reloadFilter from <filter> & <filter-mapping> from web.xml doc.
 */
private def removeReloadFilters(def portletWebXml) {
    def filterMapping = portletWebXml.'filter-mapping'.find {
        it.'filter-name' == 'reloadFilter'
    }

    filterMapping?.replaceNode({})


    def filter = portletWebXml.'filter'.find {
        it.'filter-name' == 'reloadFilter'
    }

    filter?.replaceNode({})
}



/**
 * Adds Liferay's <listener> element to web.xml doc.
 */
private def addListenerElement(def portletWebXml) {
    def listeners = portletWebXml.'listener'

    // The last <listener> entry in our file.
    def lastListener = listeners[listeners.size() - 1]

    //
    // Appends Liferay-specific taglib to web.xml
    //
    lastListener + {
        listener {
            'listener-class'('com.liferay.portal.kernel.servlet.PortletContextListener')
        }
    }

}


/**
 *  Changes the <display-name> element in web.xml doc.
 */
private def changeDisplayName(def portletWebXml, String name) {
    portletWebXml.'display-name' = name
}


/**
 *  Changes the <Context-param> value for the webAppRootKey.
 */
private def changeWebAppRootKey(def portletWebXml) {
    def node = portletWebXml.'context-param'.find {
        it.'param-name' == 'webAppRootKey'
    }

    String currentValue = node?.'param-value'.text()

    // Replace any text containing 'development' to 'production'. This doesn't
    // do anything, but it's here just for consistency.
    if (currentValue != null) {
        String newValue = currentValue.replace('development', 'production')
        node?.'param-value' = newValue
    }

}

/**
 * Adds Liferay-specific <filter> elements to web.xml doc.
 */
private def addLiferayFilters(def portletWebXml) {
    def filters = portletWebXml.'filter'

    // The last <filter> entry in our file.
    def lastFilter = filters[filters.size() - 1]

    //
    // Appends Liferay-specific <filter> to web.xml
    //
    lastFilter + {

        filter {
            'filter-name'('Cache Filter - Resource')
            'filter-class'('com.liferay.portal.kernel.servlet.PortalClassLoaderFilter')
            'init-param' {
                'param-name'('filter-class')
                'param-value'('com.liferay.portal.servlet.filters.cache.CacheFilter')
            }
            'init-param' {
                'param-name'('pattern')
                'param-value'('2')
            }
        }

        filter {
            'filter-name'('Cache Filter - Resource CSS JSP')
            'filter-class'('com.liferay.portal.kernel.servlet.PortalClassLoaderFilter')
            'init-param' {
                'param-name'('filter-class')
                'param-value'('com.liferay.portal.servlet.filters.cache.CacheFilter')
            }
            'init-param' {
                'param-name'('url-regex-pattern')
                'param-value'('.+/css\\.jsp')
            }
            'init-param' {
                'param-name'('pattern')
                'param-value'('2')
            }
        }

        filter {
            'filter-name'('Header Filter')
            'filter-class'('com.liferay.portal.kernel.servlet.PortalClassLoaderFilter')
            'init-param' {
                'param-name'('filter-class')
                'param-value'('com.liferay.portal.servlet.filters.header.HeaderFilter')
            }
            'init-param' {
                'param-name'('Cache-Control')
                'param-value'('max-age=315360000, public')
            }
            'init-param' {
                'param-name'('Expires')
                'param-value'('315360000')
            }
        }


        filter {
            'filter-name'('Header Filter - CSS JSP')
            'filter-class'('com.liferay.portal.kernel.servlet.PortalClassLoaderFilter')
            'init-param' {
                'param-name'('filter-class')
                'param-value'('com.liferay.portal.servlet.filters.header.HeaderFilter')
            }
            'init-param' {
                'param-name'('url-regex-pattern')
                'param-value'('.+/css\\.jsp')
            }
            'init-param' {
                'param-name'('Cache-Control')
                'param-value'('max-age=315360000, public')
            }
            'init-param' {
                'param-name'('Expires')
                'param-value'('315360000')
            }
        }

        filter {
            'filter-name'('GZip Filter')
            'filter-class'('com.liferay.portal.kernel.servlet.PortalClassLoaderFilter')
            'init-param' {
                'param-name'('filter-class')
                'param-value'('com.liferay.portal.servlet.filters.gzip.GZipFilter')
            }
        }

        filter {
            'filter-name'('Minifier Filter')
            'filter-class'('com.liferay.portal.kernel.servlet.PortalClassLoaderFilter')
            'init-param' {
                'param-name'('filter-class')
                'param-value'('com.liferay.portal.servlet.filters.minifier.MinifierFilter')
            }
        }

        filter {
            'filter-name'('Minifier Filter - CSS JSP')
            'filter-class'('com.liferay.portal.kernel.servlet.PortalClassLoaderFilter')
            'init-param' {
                'param-name'('filter-class')
                'param-value'('com.liferay.portal.servlet.filters.minifier.MinifierFilter')
            }
            'init-param' {
                'param-name'('url-regex-pattern')
                'param-value'('.+/css\\.jsp')
            }
        }


    }


}

/**
 * Adds Liferay-specific <filter-mapping> elements to web.xml doc.
 */
private def addLiferayFilterMappings(def portletWebXml) {
    def filterMappings = portletWebXml.'filter-mapping'

    // The last <filter-mapping> entry in our file.
    def lastFilterMap = filterMappings[filterMappings.size() - 1]

    //
    // Appends <filter-mapping>'s to web.xml
    //
    lastFilterMap + {

        'filter-mapping' {
            'filter-name'('Cache Filter - Resource')
            'url-pattern'('*.css')
        }

        'filter-mapping' {
            'filter-name'('Cache Filter - Resource')
            'url-pattern'('*.html')
        }

        'filter-mapping' {
            'filter-name'('Cache Filter - Resource')
            'url-pattern'('*.js')
        }

        'filter-mapping' {
            'filter-name'('Cache Filter - Resource CSS JSP')
            'url-pattern'('*.jsp')
        }

        'filter-mapping' {
            'filter-name'('Header Filter')
            'url-pattern'('*.css')
        }

        'filter-mapping' {
            'filter-name'('Header Filter')
            'url-pattern'('*.gif')
        }

        'filter-mapping' {
            'filter-name'('Header Filter')
            'url-pattern'('*.html')
        }

        'filter-mapping' {
            'filter-name'('Header Filter')
            'url-pattern'('*.ico')
        }

        'filter-mapping' {
            'filter-name'('Header Filter')
            'url-pattern'('*.jpg')
        }

        'filter-mapping' {
            'filter-name'('Header Filter')
            'url-pattern'('*.js')
        }

        'filter-mapping' {
            'filter-name'('Header Filter')
            'url-pattern'('*.png')
        }

        'filter-mapping' {
            'filter-name'('Header Filter - CSS JSP')
            'url-pattern'('*.jsp')
        }

        'filter-mapping' {
            'filter-name'('GZip Filter')
            'url-pattern'('*.css')
        }

        'filter-mapping' {
            'filter-name'('GZip Filter')
            'url-pattern'('*.html')
        }

        'filter-mapping' {
            'filter-name'('GZip Filter')
            'url-pattern'('*.js')
        }

        'filter-mapping' {
            'filter-name'('GZip Filter')
            'url-pattern'('*.jsp')
        }

        'filter-mapping' {
            'filter-name'('Minifier Filter')
            'url-pattern'('*.css')
        }

        'filter-mapping' {
            'filter-name'('Minifier Filter')
            'url-pattern'('*.js')
        }

        'filter-mapping' {
            'filter-name'('Minifier Filter - CSS JSP')
            'url-pattern'('*.jsp')
        }

    }

}

/**
 * Saves the Xml document to file.
 */
private def saveXmlToFile(String filename, def portletWebXml) {

    def builder = new StreamingMarkupBuilder()

    def newVersion = builder.bind {

        // Note: If we don't declare the namespace, markup builder prepends a 'tag0:'
        // namespace to every XML element when we write out the file!!
        mkp.declareNamespace("": "http://java.sun.com/xml/ns/j2ee")

        mkp.yield(portletWebXml)
    }

    // Convert single quotes to double quotes
    def result = newVersion.toString().replace('\'', '"')

    def xmlFile = new File(filename)
    if (xmlFile.exists()) xmlFile.delete()
    xmlFile.write(result)

}



private def buildWebXmlFile(String originalWebXmlFile, String newWebXmlFile,
                            String portletName, String grailsAppName) {

    def webXml = readWebXmlFile(originalWebXmlFile)

    fixTagLibTags(webXml)
    removePlutoArtifacts(webXml, portletName)
    addListenerElement(webXml)
    removeReloadFilters(webXml)
    changeDisplayName(webXml, grailsAppName)
    changeWebAppRootKey(webXml)
    addLiferayFilters(webXml)
    addLiferayFilterMappings(webXml)

    saveXmlToFile(newWebXmlFile, webXml)
}

/**
 * Modifies the web.xml file with Liferay-specific stuff.
 */
def modifyWebXml(String webInfDir, String grailsAppName) {
    // The officially deployed web.xml file
    String webXmlFileName = "${webInfDir}/web.xml"
    def webXmlFile = new File(webXmlFileName)

    // A backup copy of the web.xml file before we do Liferay mods to it.
    String backupFileName = "${webInfDir}/original-web.xml"
    def backupFile = new File(backupFileName)

    // Ensure we have a backup of original web.xml; leave it alone if we have a backup already.
    // Otherwise, make a new backup before we muck up the web.xml file.
    if (!backupFile.exists()) {
        if (!webXmlFile.exists()) {
            println "Fatal error! ${webXmlFileName} does not exist on your system."
        }
        ant.copy(file: webXmlFileName, tofile: backupFileName)
    }

    // If a web.xml file exists, delete it so we don't get confused. We will create a new one.
    if (webXmlFile.exists()) webXmlFile.delete()

    // Get portlets from our app's /controllers directories.
    // note: Strip off the 'Portlet.groovy' suffix from filename
    String[] allPortlets = findPortlets()
    if (allPortlets == []) {
        println "Error! No portlets found to process."
        System.exit(-1)
    }

    // HACK! Use only 1st portlet. XXX This will require more processing to support
    // multiple portlets at same time.
    String portletName = allPortlets[0]

    // Build Liferay-specific web.xml file from web.xml processed by grails and the porlets plugin.
    buildWebXmlFile(backupFileName, webXmlFileName, portletName, grailsAppName)


}

def createContextXml(File deployDir, String filename, String pathAttr, String docBaseAttr, boolean reloadableAttr) {
    def xmlWriter = new StreamingMarkupBuilder()

    def xml = xmlWriter.bind {
        'Context'(reloadable: reloadableAttr, path: pathAttr, docBase: docBaseAttr)
    }

    def xmlFile = new File(deployDir, filename)
    if (!deployDir.exists()) {
        deployDir.mkdirs()
    } else if (xmlFile.exists()) {
        xmlFile.delete()
    }
    String reformatted = xml.toString().replace('\'', '"')

    xmlFile.write(reformatted)
}

//---------------------------------------


target(main: "Deploys exploded war's context file to LIFERAY_DEPLOY") {

    depends(parseArguments, war)

    if (!liferayHome) {
        logErrorAndExit('Fatal Error! Must set environment variable $LIFERAY_HOME for this plugin.',
                new RuntimeException('Missing $LIFERAY_HOME variable'))
    }
    if(!new File(liferayHome).exists()){
         logErrorAndExit('Fatal Error! $LIFERAY_HOME is not a valid directory.',
                new RuntimeException('Invalid $LIFERAY_HOME variable'))
    }


    wipeExplodedWarDirectory(explodedDir)

    // take new war file and unzip it to a new exploded dir.
    ant.unjar(src: warName, dest: explodedDir)

    generateLiferayArtifacts(metaInfDir, webInfDir)

    modifyWebXml(webInfDir, grailsAppName)

    File deployDir = new File("${liferayHome}/deploy/")
    String filename = "${grailsAppName}.xml"

    //ant.echo("Creating filename: $filename")

    // create context.xml file for our fast deployment
    createContextXml(deployDir, filename, grailsAppName, explodedDir, true)
}

setDefaultTarget(main)
