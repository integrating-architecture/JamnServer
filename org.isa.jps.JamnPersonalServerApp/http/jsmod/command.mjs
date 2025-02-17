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
	wsoRefId;
	
	initialize() {	
		super.initialize();
				
		this.runButton.innerHTML = this.commandName;
		this.runButton.addEventListener("click", (evt) => {
			this.runCommand();
			evt.stopImmediatePropagation();
		});

		this.runArgs.setAttribute("placeholder", this.commandDef.args);
		this.runArgs.setAttribute("disabled", this.commandDef.args=="<none>");
		
		this.wsoRefId = newSimpleId(this.id+":");		
		WbApp.addWsoMessageListener((wsoMsg) => {
				
			if(wsoMsg.hasReference(this.wsoRefId)){
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
		
		this.isInitialized = true;
	}

	runCommand() {
		let wsoMsg = new WsoCommonMessage(this.wsoRefId);
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
