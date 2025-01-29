import {echo, sh, workspacePath} from "tools.mjs";

/**
 * A playful "JS build script" example to build the JamnServer project 
 * with git and maven in the workspace folder.
 *  
 * Note: since the default server port (8099) is used by the unit tests, 
 * the executing JamnPersonalServerApp must use a different port.
 * 
 * Run on the JPS CLI console jps> runjs sample/build-project.mjs 
 */
let projectName = "JamnServer"
let projectGitUrl = `https://github.com/integrating-architecture/${projectName}.git`;
let workspace = workspacePath();
let wsLocalMvnRepo = workspacePath(".m2ws");


//print build output to console
let outputConsumer = (line) => {
	console.log(line);
};

function buildProject(){
	
	echo(`Start build project [${projectName}] from [${projectGitUrl}]`);
	
	//clear workspace dir
	sh(`rd /s /q ${projectName}`, workspace, outputConsumer);
	
	//clone git project
	sh(`git clone ${projectGitUrl}`, workspace, outputConsumer);
	
	//call maven install
	//using a workspace local .m2 repo
	sh(`mvn "-Dmaven.repo.local=${wsLocalMvnRepo}" install`
		, workspacePath(projectName)
		, outputConsumer);

}

buildProject();
