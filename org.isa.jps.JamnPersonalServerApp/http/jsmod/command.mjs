/* Authored by www.integrating-architecture.de */

import { NL, newSimpleId } from '../jsmod/tools.mjs';
import { WsoCommonMessage } from '../jsmod/data-classes.mjs';
import { BaseCommandView } from '../jsmod/view-classes.mjs';

/**
 * Concrete view class for this command component
 */
class CommandView extends BaseCommandView {
	
	//the id used to identify wso sender/receiver
	//between client und server
	refId;
	
	onInstallation(installKey, installData) {
		this.id = installKey;
	}

	initialize() {	
		super.initialize();
		
		this.setTitle(this.commandDef.title);
		
		this.runButton = this.getElement("pbRun");
		this.runIndicator = this.getElement("run.indicator");
		this.runArgs = this.getElement("cmd.args");
		this.outputArea = this.getElement("cmd.output");
		
		this.runButton.innerHTML = this.commandName;
		this.runButton.addEventListener("click", (evt) => {
			this.runCommand();
			evt.stopImmediatePropagation();
		});

		this.runArgs.setAttribute("placeholder", this.commandDef.args);
		this.runArgs.setAttribute("disabled", this.commandDef.args=="<none>");
		
		if(!this.refId){
			this.refId = newSimpleId(this.commandDef.script+":");		
			WbApp.addWsoMessageListener((wsoMsg) => {
					
				if(wsoMsg.hasReference(this.refId)){
					if(wsoMsg.hasStatusSuccess()){
						this.setRunning(false);
						this.addOutputLine(NL+"Command finished: [" + wsoMsg.status + "] ["+this.commandName+"]");
					}else if(wsoMsg.hasStatusError()){
						this.addOutputLine(NL+wsoMsg.error);
						this.setRunning(false);
					}else{
						this.addOutputLine(wsoMsg.textdata);
					}
				}
			});		
		}
		//this.isInitialized = true;
	}

	runCommand() {
		let wsoMsg = new WsoCommonMessage(this.refId);
		wsoMsg.command = this.commandDef.command;
		wsoMsg.script = this.commandDef.script;
		
		this.clearOutput();
		WbApp.sendWsoMessage(wsoMsg, ()=>{
			this.setRunning(true);
		});
	}		
}

//export this view component as instances
//because command views are individual wso message receiver
export function getView(){
	return new CommandView("commandView", "/jsmod/command.html");
} 

/**
 * Internals
 */
/**
 * The component run method
 */

/*
function runCommand(view) {
	let wsoMsg = new WsoCommonMessage(view.refId);
	wsoMsg.command = view.commandDef.command;
	wsoMsg.script = view.commandDef.script;
	
	view.clearOutput();
	WbApp.sendWsoMessage(wsoMsg, ()=>{
		view.setRunning(true);
	});
}



let wsoMessageListener = (wsoMsg) => {
		
	if(wsoMsg.hasReference(viewInstance.refId)){
		if(wsoMsg.hasStatusSuccess()){
			viewInstance.setRunning(false);
			viewInstance.addOutputLine(NL+"Command finished: [" + wsoMsg.status + "] ["+viewInstance.commandName+"]");
		}else if(wsoMsg.hasStatusError()){
			viewInstance.addOutputLine(NL+wsoMsg.error);
			viewInstance.setRunning(false);
		}else{
			viewInstance.addOutputLine(wsoMsg.textdata);
		}
	}
};
*/