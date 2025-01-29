import {sh, LS} from "tools.mjs";

let val = "";
let workDir = null;
let cmd = "dir";

console.log(args);

if(args && args.length>=1){
	cmd = args[0];
	if(args.length>=2){
		workDir = args[1];
	}
}

if(workDir!=null){
	val = sh(cmd, workDir);
}else{
	val = sh(cmd);	
}

val.join(LS);