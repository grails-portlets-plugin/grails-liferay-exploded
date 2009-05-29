class LiferayExplodedGrailsPlugin {

    def version = "0.7"

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.1 > *"

    // the other plugins this plugin depends on
    def dependsOn = [portlets:"0.3 > *", portletsLiferay:"0.1 > *"]

    // This plugin only is used in 'development' environment
    def environments = ['dev', 'development']

    def scopes = [excludes: 'war']
                                             
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def author = "Brian McGinnis"
    def authorEmail = "brian_mcginnis@diva-america.com"
    def title = "Plugin that creates fast, temporary deployment artifacts for Liferay."
    def description = '''\\
This plugin generates Liferay-specific artifacts for exploded war deployment.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/liferay-exploded"

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }

    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)
    }

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional)
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
