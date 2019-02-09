import {
    extractSecrets,
    containsValidSecret
} from "../../lib/editor/text"

Meteor.methods({
    /**
      * Meteor method to get additional solutions to the current model. This
      * will call the Alloy API (webService) without creating a new model in
      * the database.
      * 
      * @param {Number} commandIndex the index of the command to execute
      * @param {Boolean} commandType whether the command was a run (true) or
      *     check (false)
      * @param {String} currentModelId the id of the current model (from which
      *     the new will derive)
      * 
      * @returns the instance data and the id of the new saved model
      */
    nextInstances: function(code, commandIndex, commandType, currentModelId) {
        return new Promise((resolve, reject) => {
            // must send model in case session has expired and must be restarted
            // if no secrets, try to extract from original
            let code_with_secrets = code
            if (currentModelId && !containsValidSecret(code)) {
                let o = Model.findOne(currentModelId).original
                code_with_secrets = code + extractSecrets(Model.findOne(o).code).secret                    
            }

            // call webservice to get instances
            HTTP.call('POST', `${Meteor.settings.env.API_URL}/getInstances`, {
                data: {
                    model: code_with_secrets,
                    numberOfInstances: Meteor.settings.env.MAX_INSTANCES,
                    commandIndex: commandIndex,
                    sessionId: currentModelId
                }
            }, (error, result) => {
                if (error) reject(error)

                let content = JSON.parse(result.content);
                // if unsat, still list with single element
                let sat
                Object.keys(content).forEach(k => {
                    content[k].commandType = commandType;
                    sat = content[k].unsat;
                });

                // resolve the promise
                resolve({
                    instances: content,
                    newModelId: currentModelId
                });
            });
        })
    }
});