/* Authored by www.integrating-architecture.de */

import { LS, echo, sh, workspacePath, isOnUnix, ArgumentProcessor } from "tools.mjs";

/**
 * A test script to play around.
 * 
 * Hint: this script is also called in a junit test
 * but the test will always be passed
 * 
 * see also: \http\jsmod\sidebar-content.mjs
 */
let helpText = `
Command Help:
Testscript - running a shell command from serverside javascript.
The script first checks if the underlaying platform is Windows or Unix
to then execute the corresponding commands for actual directory and directory content. 

Args: <none>
`;
let workspace = workspacePath();

//for example 
//useing a callback to consume/pipe script output during execution
let scriptResult = [];
let outputConsumer = (line) => {
	scriptResult.push(line);
};

//a main function
function run() {

	//process args if any
	if (new ArgumentProcessor().process(args, {
		"-h": (proc) => { echo(helpText); proc.directReturn = true; }
	}).directReturn) {
		//for help arg
		//return without processing the script itself
		return scriptResult;
	}

	//setup shell commands for the current platform
	let test = isOnUnix() ? { cmd: { pwd: "pwd", ls: "ls" }, os: "unix" } : { cmd: { pwd: "cd", ls: "dir" }, os: "windows" };

	//print out some infos
	echo([
		`Start: [${new Date().toUTCString()}]`,
		`Commands: actual-path: [${test.cmd.pwd}], actual-dir-content: [${test.cmd.ls}], Platform: ${test.os}`,
		`Args: ${args}`,
		""
	].join(LS));

	//run shell commands in workspace dir
	sh(test.cmd.pwd, workspace, outputConsumer);
	sh(test.cmd.ls, workspace, outputConsumer);

	return scriptResult;
}


//return joined output as script return value
run().join(LS);
