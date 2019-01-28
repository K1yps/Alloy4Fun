import {Instance} from "../../lib/collections/instance"

Meteor.methods({
    /**
      * Meteor method to store a model instance with a user-defined theme.
      * Creates an instance that points to the model. Saves the theme in the
      * instance itself.
      * 
      * @param {String} modelId the id of the model that originated the instance
      * @param {String} command the index of the command that was executed
      * @param {String} instance the JSON string of the cytoscape graph
      * @param {Object} themeData the theme information for cytoscape
      * 
      * @return the id of the new instance
      */
    storeInstance: function(modelId, command, instance, themeData) {
        return Instance.insert({
            model_id: modelId,
            command: command,
            graph: instance,
            theme: themeData,
            time: new Date().toLocaleString()
        });
    }
});