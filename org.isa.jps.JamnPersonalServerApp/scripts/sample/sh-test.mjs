/* Authored by www.integrating-architecture.de */

import {LS, echo, sh, workspacePath, isOnUnix} from "tools.mjs";

/**
 * A test script to play around.
 * 
 * Hint: this script is also called in a junit test
 * but the test will always be passed
 * 
 * see also: \http\jsmod\sidebar-content.mjs
 */
let testDescr = "Test - run a shell command from javascript"
let workspace = workspacePath();

//for example 
//use a callback to consume function output
let returnValue = [];
let outputConsumer = (line) => {
	returnValue.push(line);
};

function run(){

	//setup shell commands for the current platform
	let test = isOnUnix() ? {cmd: {pwd:"pwd", ls:"ls"}, os:"unix"} : {cmd: {pwd:"cd", ls:"dir"}, os:"windows"};

	echo([
		`Start [${testDescr}] [${new Date().toUTCString()}]`,
		`Commands: actual-path: [${test.cmd.pwd}], actual-dir-content: [${test.cmd.ls}], Platform: ${test.os}`,
		""
	].join(LS));
	
	//run shell cmds in workspace dir
	sh(test.cmd.pwd, workspace, outputConsumer);
	sh(test.cmd.ls, workspace, outputConsumer);
	
	return returnValue;
}

//return joined output as script return value
run().join(LS);
