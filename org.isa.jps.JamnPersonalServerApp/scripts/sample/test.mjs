import {echo, sh, workspacePath} from "tools.mjs";

/**
 * A test script to play around.
 */
let projectName = "Test"
let workspace = workspacePath();


//print build output to console
let outputConsumer = (line) => {
	console.log(line);
};

function run(){
	
	echo(`Start [${projectName}] [${new Date().toUTCString()}]`);
	
	//show workspace dir
	sh(`dir`, workspace, outputConsumer);
	
}

run();
