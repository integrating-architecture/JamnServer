/* Authored by www.integrating-architecture.de */

import { NL, getChildOf, setVisibility, setDisplay } from '../jsmod/tools.mjs';
import { ViewSource, CommandDef } from '../jsmod/data-classes.mjs';

/**
 * View base classes.
 * 
 * View classes encapsulate a basic structure and a basic open close procedure 
 * in the form of corresponding methods and flags.
 * 
 */
export class BaseView  {
	
	id = "";
	viewSource = new ViewSource("");
	//the view html dom element
	viewElement = null;
	viewTitle = null;
	viewWorkarea = null;

	//header elements
	closeIcon = null;
	collapseIcon = null;
	pinIcon = null;

	headerMenu = null;

	isInitialized = false;
	isRunning = false;
	isOpen = false;
	isPinned = false;
	isCollapsed = false;
		
	constructor(id, file){
		this.id = id;
		this.viewSource = new ViewSource(file);
		
		this.isInitialized = false;
		this.isRunning = false;
		this.isOpen = false;		
	}
		
	open(data=null) {	
		if(!this.isInitialized){
			this.initialize();
		}
		this.writeDataToView();
		setVisibility(this.viewElement, true);
		this.isOpen = true;		
	}
		
	close() {
		this.isOpen = false;
		return this.isCloseable();
	}	

	isCloseable(ctxObj=null) {
		return !(this.isRunning || this.isPinned);
	}

	initialize(){	
		//to be overwritten
		//only called when isInitialized=false
		this.viewElement = document.getElementById(this.id);	
		this.viewTitle = this.getElement("view.title");
		this.viewWorkarea = this.getElement("work.view.workarea");
		
		this.closeIcon = this.getElement("close.icon");
		this.pinIcon = this.getElement("pin.icon");
		this.collapseIcon = this.getElement("collapse.icon");
		
		let elem = this.getElement("header.menu"); 
		if(elem && this.closeIcon){
			this.headerMenu = new ViewHeaderMenu(elem);
			
			this.headerMenu.addItem("Close", (evt)=>{
				this.closeIcon.click();
			});
		}
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

	togglePinned() {
		this.isPinned = !this.isPinned;
		
		if(this.isPinned){
			this.pinIcon.classList.remove("bi-pin"); 
			this.pinIcon.classList.add("bi-pin-angle");
			this.pinIcon.title = "Unpin view";
			this.closeIcon.style["pointer-events"]="none";
		}else{
			this.pinIcon.classList.remove("bi-pin-angle"); 
			this.pinIcon.classList.add("bi-pin");	
			this.pinIcon.title = "Pin to keep view";
			this.closeIcon.style["pointer-events"]="all";				
		}		

		return this.isPinned;
	}

	toggleCollapsed() {
		this.isCollapsed = !this.isCollapsed;
				
		if(this.isCollapsed){
			this.collapseIcon.classList.remove("bi-chevron-bar-contract"); 
			this.collapseIcon.classList.add("bi-chevron-bar-expand");
			this.collapseIcon.title = "Expand view";
		}else{
			this.collapseIcon.classList.remove("bi-chevron-bar-expand"); 
			this.collapseIcon.classList.add("bi-chevron-bar-contract");	
			this.collapseIcon.title = "Collapse  view";
		}		

		setDisplay(this.viewWorkarea, !this.isCollapsed);

		return this.isCollapsed;
	}

	toggleHeaderMenu() {
		this.headerMenu.toggleVisibility();
	}

}


/**
 * A Base class for command views
 * that get parameterized with a commandDef object on installation
 */
export class BaseCommandView  extends BaseView {
	
	commandDef = new CommandDef();
	commandName = "";
	
	runButton = null;
	runIndicator  = null;
	runArgs  = null;
	outputArea = null;
			
	onInstallation(installKey, installData) {
		this.id = installKey;
		if(installData instanceof CommandDef){
			this.commandDef = installData;
			this.commandName = this.commandDef.command+" "+this.commandDef.script;
		}
	}

	initialize() {	
		super.initialize();
		
		this.setTitle(this.commandDef.title);
		
		this.runButton = this.getElement("pb.run");
		this.runIndicator = this.getElement("run.indicator");
		this.runArgs = this.getElement("cmd.args");
		this.outputArea = this.getElement("cmd.output");
		
		if(this.headerMenu){			
			this.headerMenu.addItem("Clear Output", (evt)=>{
				this.clearOutput();
			});
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
}

/**
 */
export class ViewHeaderMenu {
	
	containerElem = null;
	iconElem = null;
	menuElem = null;
	isVisible = false;
	
	constructor(containerElem){
		this.containerElem = containerElem; 
		this.iconElem =  containerElem.children[0];
		this.menuElem =  containerElem.children[1];
		
		window.addEventListener("click", (event) =>{
			this.onAnyWindowClick(event)
		});
	}
	
	hasItems(){
		return this.menuElem?.children.length>0;
	}
	
	addItem(text, cb, id=""){
		let item = document.createElement("a");
		item.id = id;
		item.href="javascript:void(0)";
		item.innerHTML = text;
		item.onclick = (evt)=>cb(evt);
		
		this.menuElem.appendChild(item);
	}
	
	toggleVisibility(){
		if(this.hasItems()){
			this.isVisible = !this.isVisible
			setDisplay(this.menuElem, this.isVisible);
		}
	}
	
	onAnyWindowClick(event){
		if(this.isVisible){
			this.toggleVisibility();
		}
	}

}