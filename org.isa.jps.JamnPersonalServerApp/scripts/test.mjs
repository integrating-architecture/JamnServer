import * as tool from "tools.mjs";

let projectName = "JamnServer"
let projectGitUrl = `https://github.com/integrating-architecture/${projectName}.git`;
let workspace = tool.workspacePath();
let wsLocalMvnRepo = tool.workspacePath(".m2ws");
let outputData = [];


//print build output to console
let outputConsumer = (line) => {
	console.log(line);
};

function buildProject(){
	
	//clear workspace dir
	tool.sh(`rd /s /q ${projectName}`, workspace, outputConsumer);
	//clone git project
	tool.sh(`git clone ${projectGitUrl}`, workspace, outputConsumer);
	//call maven install
	//using a workspace local .m2 repo
	tool.sh(`mvn "-Dmaven.repo.local=${wsLocalMvnRepo}" install`
		, tool.workspacePath(projectName)
		, outputConsumer);
}

buildProject();


outputData.join(tool.LS);