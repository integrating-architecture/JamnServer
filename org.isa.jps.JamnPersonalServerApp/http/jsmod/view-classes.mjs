/* Authored by www.integrating-architecture.de */

import { NL, getChildOf, getViewHtml, setVisibility } from '../jsmod/tools.mjs';
import { ViewSource, CommandDef } from '../jsmod/data-classes.mjs';

/**
 * View base classes.
 * 
 * View classes encapsulate a basic structure and a basic open close procedure 
 * in the form of corresponding methods and flags.
 * 
 * The main feature of the view construction is loading the view HTML code on demand.
 */
export class BaseView  {
	
	id = "";
	viewSource = new ViewSource("");
	//the view html dom element
	viewElement = null;
	viewTitle = null;

	isInitialized = false;
	isRunning = false;
	isOpen = false;
	
	//an external object that manages the views
	//in this case the workbench
	viewManager = null;
		
	constructor(id, file){
		this.id = id;
		this.viewSource = new ViewSource(file);
		
		this.isInitialized = false;
		this.isRunning = false;
		this.isOpen = false;		
	}
		
	open(data) {		
		if(!this.isOpen){
			//at this point the html code is loaded
			getViewHtml(this.viewSource, (html)=>{
				this.showView(html);
			});
		}		
	}
	
	close() {
		if(!this.isRunning && this.isOpen){
			if(this.viewManager){
				this.viewManager.close(this);
			}
			this.isOpen=false;
			return true;
		}
		return false;
	}	
	
	isCloseable(ctxObj=null) {
		if(this.isRunning){
			return false;
		}
		return ctxObj?.view !== this;
	}

	setViewManager(manager){	
		this.viewManager = manager;
	}

	showView(html){	
		//at this point the dom is creted by the viewManager
		//up to this point the view is just html text code
		if(this.viewManager){
			this.viewManager.open(this, html);
		}

		if(!this.isInitialized){
			this.initialize();
		}
		this.writeDataToView();
		setVisibility(this.viewElement, true);
		this.isOpen = true;
	}	
	
	initialize(){	
		//to be overwritten
		//only called when isInitialized=false
		this.viewElement = document.getElementById(this.id);	
		this.viewTitle = this.getElement("view.title");
	}

	writeDataToView(){
		//to be overwritten
		//always called in showView
	}	

	readDataFromView(){
		//to be overwritten
	}	
	
	setRunning(flag) {
		this.isRunning = flag;
	}
	
	setTitle(title){
		this.viewTitle.innerHTML = title;
	}	
	
	getElement(id){
		return getChildOf(this.viewElement, id);
	}	
	
	onInstallation(installKey, installData) {
	}

}


/**
 * A Base class for command views
 * that get parameterized with a commandDef object
 */
export class BaseCommandView  extends BaseView {
	
	commandDef = new CommandDef();
	commandName = "";
	
	runButton = null;
	runIndicator  = null;
	runArgs  = null;
	outputArea = null;
		
	open(commandDef) {		
		if(!this.isOpen){
			this.commandDef = commandDef;
			this.commandName = this.commandDef.command+" "+this.commandDef.script;
			super.open();
		}		
	}

	clearOutput() {
		let lastValue = this.outputArea.value;
		this.outputArea.value = "";
		return lastValue;
	}
	
	addOutputLine(line) {
		this.outputArea.value += line + NL;
		this.outputArea.scrollTop = this.outputArea.scrollHeight;
	}
	
	setRunning(flag) {
		this.isRunning = flag;
		
		setVisibility(this.runIndicator, flag);
		this.runButton.disabled = flag;
		
		if(flag){
			this.runButton.classList.remove("cmd-button");
			this.runButton.classList.add("cmd-button-disabled");
		}else{
			this.runButton.classList.remove("cmd-button-disabled");
			this.runButton.classList.add("cmd-button");
		}
	}
	
	isCloseable(ctxObj=null) {
		if(this.isRunning){
			return false;
		}
		
		if(ctxObj?.view !== this){
			return true;
		}	
			
		return ctxObj?.data !== this.commandDef;
	}
}

