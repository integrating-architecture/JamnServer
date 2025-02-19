import {LS, echo, sh, workspacePath, isOnUnix} from "tools.mjs";

/**
 * A test script to play around.
 */
let testDescr = "Test - run a shell command from javascript"
let workspace = workspacePath();


//print output to console
let outputConsumer = (line) => {
	console.log(line);
};

function run(){

	let def = isOnUnix() ? {cmd: "ls", os:"unix"} : {cmd: "dir", os:"windows"};
	echo([
		`Start [${testDescr}] [${new Date().toUTCString()}]`,
		`Command: ${def.cmd}, Platform: ${def.os}`,
		""
	].join(LS));
	
	//show workspace dir
	sh(def.cmd, workspace, outputConsumer);
}

run();
