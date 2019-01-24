/**
 * Utility functions for parsing and analysis of code of Alloy models, shared
 * by both client and server.
 */

export {
    containsValidSecret,
    getCommandsFromCode,
    secretTag,
    paragraphKeywords,
    extractSecrets
}

/** The secret tag used in Alloy code. */
secretTag = "//SECRET"
/** The keywords that identify paragraphs. */
paragraphKeywords = "sig|fact|assert|check|fun|pred|run"

/**
  * Checks whether a the code of an Alloy model contains some valid 'secret' tag
  * (i.e., a line exactly the tag secret). No white-spaces allowed before/after.
  * 
  * @param {String} code the Alloy model with the potential secret
  * 
  * @return true if there is a secrete tag 
  */
function containsValidSecret(code) {
    return (extractSecrets(code).secret != "")
}

/**
  * Splits the Alloy code of a model between public and private paragraphs.
  * Private paragraphs are preceeded by a secret tag.
  * 
  * @param {String} code the complete code with possible secrets
  * @return the public and private paragraphs of the code 
  */
function extractSecrets(code) {
    let secret = "",
        public_code = "";
    let s, i;
    let tag = secretTag.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    let pgs = paragraphKeywords
    let pgd = `(?:(?:one|abstract|lone|some)\\s+)*${pgs}`
    let exp = `(?:\\/\\*(?:.|\\n)*?\\*\/|(${tag}\\s*?\\n\\s*(?:${pgd})(?:.|\\n)*?)(?:${tag}\\s*?\\n\\s*)?(?:(?:${pgd})\\s|$))`
    while (s = code.match(RegExp(exp))) {
        if (s[0].match(/^\/\*(?:.|\n)*?\*\/$/)) {
            i = code.indexOf(s[0]);
            public_code += code.substr(0, i + s[0].length);
            code = code.substr(i + s[0].length);
        } else {
            i = code.indexOf(s[0]);
            public_code += code.substr(0, i);
            secret += s[1];
            code = code.substr(i + s[1].length);
        }
    }
    public_code += code; 
    return {
        public: public_code,
        secret: secret
    };
}

/**
 Calculates a list of identifiers for the run/check commands defined in the
 code of an Alloy model. If named, returns name, otherwise, returns indexed
 "run$"/"check$".
 
 @param {String} code the Alloy model to be analysed

 @return a list of identifiers for commands in the code
 */
function getCommandsFromCode(code) {
    let pattern = /((\W|^)run(\{|(\[\n\r\s]+\{)|([\n\r\s]+([^{\n\r\s]*)))|((\W|^)check(\{|(\[\n\r\s]+\{)|([\n\r\s]+([^{\n\r\s]*)))))/g;
    let commands = [];
    let commandNumber = 1;

    // To avoid commands that are in comment, comments must be eliminated
    // before parse
    code = code.replace(/\/\/(.*)(\n)/g, "");
    let matches = pattern.exec(code);

    while (matches != null) {
        let pre = matches[0].includes("run") ? "run " : "check "
        if (matches[6]) commands.push(pre + matches[6]);
        else if (matches[12]) commands.push(pre + matches[12]);
        else commands.push(pre + commandNumber);
        commandNumber++;
        matches = pattern.exec(code);
    }

    return commands
}