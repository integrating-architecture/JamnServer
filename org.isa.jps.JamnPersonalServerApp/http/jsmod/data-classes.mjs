/* Authored by www.integrating-architecture.de */

/**
 * Some simple data classes.
 * 
 * The classes serve to document central data structures.
 */
export class WsoCommonMessage {
	reference = "";
	textdata = "";
	command = "";
	script = "";
	status = "";
	error = "";
	
	constructor(reference) {
		this.reference = reference;
	}
	
	hasReference(id){
		return this.reference===id;
	}

	hasStatusSuccess(){
		return "success" === this.status.toLowerCase();
	}
	
	hasStatusError(){
		return "error" === this.status.toLowerCase();
	}

	setStatusError(errorInfo){
		this.status = "error";
		this.error = errorInfo;
	}

};

export class CommandDef {
	title = "";
	command = "";
	script = "";
	args = "<none>";
	
	constructor(title="", command="", script="", args="") {
		this.title = title;
		this.command = command;
		this.script = script;
		this.args = args;
	}
};


export class ViewSource {
	file = "";
	html = null;
	
	constructor(file) {
		this.file = file;
	}
	
	isEmpty(){
		return this.html==null;
	}
};