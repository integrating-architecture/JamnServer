/* Authored by www.integrating-architecture.de */

import { NL, newSimpleId } from '../jsmod/tools.mjs';
import { WsoCommonMessage } from '../jsmod/data-classes.mjs';
import { BaseCommandView, CompBuilder } from '../jsmod/view-classes.mjs';

/**
 * A general View class for commands.
 * An example of 
 *  - how to use web socket communication to execute server side "commands"
 *  - and how to dynamically integrate the components html source code
 *    using standard js/dom functionality
 * 
 * In this example, a command is a server-side JavaScript.
 * 
 * The client side command definitions are located in:
 *  - sidebar-content.mjs
 * The web socket counterpart on the server-side is:
 *  - org.isa.jps.comp.DefaultWebSocketMessageProcessor
 * The server side scripts are located in subdir
 *  - scripts
 * 
 * Creating dynamic content and components
 * Jamn supports 3 principal approaches and their free combinations:
 * - static file content
 * - server side dynamically generated and/or enriched content from any source  
 * - client side dynamically loaded, manipulated, created content and fragments
 */
class CommandView extends BaseCommandView {

	//the id used to identify websocket sender/receiver
	//between client und server
	wsoRefId;

	constructor (id){
		super(id, "");
		//use this js component html view source
		this.viewSource.html = viewHtml;
	}

	initialize() {
		super.initialize();

		//using a builder to create and config UI components
		let builder = new CompBuilder();
		builder.labelStyle = { "min-width": "80px", "text-align": "left" };

		let compSet = builder.newCompSet();
		this.viewWorkarea.prepend(compSet);

		this.runButton = builder.newButtonComp({ label: "Command:" })
			.appendTo(compSet)
			.config((comp) => {
				let pb = comp.ctrl();
				pb.title = "Run command";
				pb.value = this.commandName;
				pb.onclick = (evt) => {
					this.runCommand();
				};
			});

		this.runArgs = builder.newTextComp({ label: "Args:" })
			.appendTo(compSet)
			.style(1, { width: "300px" })
			.attrb(1, {
				title: "Command arguments",
				placeholder: this.commandDef.args,
			});

		this.outputArea = builder.newTextAreaComp({ label: "Output:", clazz: ["wkv-output-textarea-ctrl"] })
			.appendTo(compSet)
			.style(1, { width: "100%", height: "400px" })
			.attrb(1, { disabled: true })
			.style({ "align-items": "flex-start", "margin-top": "10px" });


		//init websocket connection
		this.wsoRefId = newSimpleId(this.id + ":");
		WbApp.addWsoMessageListener((wsoMsg) => {

			if (wsoMsg.hasReference(this.wsoRefId)) {
				if (wsoMsg.hasStatusSuccess()) {
					this.setRunning(false);
					this.addOutputLine(NL + "Command finished: [" + wsoMsg.status + "] [" + this.commandName + "]");
				} else if (wsoMsg.hasStatusError()) {
					this.addOutputLine(NL + wsoMsg.error);
					this.setRunning(false);
				} else {
					this.addOutputLine(wsoMsg.textdata);
				}
			} else if (wsoMsg.hasStatusError && wsoMsg.error.includes("connection")) {
				this.addOutputLine(NL + wsoMsg.error);
				this.setRunning(false);
			}
		});

		this.isInitialized = true;
	}

	runCommand() {
		let wsoMsg = new WsoCommonMessage(this.wsoRefId);
		wsoMsg.command = this.commandDef.command;
		wsoMsg.script = this.commandDef.script;

		this.clearOutput();
		WbApp.sendWsoMessage(wsoMsg, () => {
			this.setRunning(true);
		});
	}
}

//export this view component as instances
//because command views are individual wso message receiver
export function getView() {
	
	//use the html code from this js module
	return new CommandView("commandView");

	//alternatively load html code from a file
	//return new CommandView("commandView", "/jsmod/command.html");
} 

/**
 * example of html source code integrated as string in this js view component module
 * the code can also be implemented in a separate file or inside another js module etc.
 */
let viewHtml = `
<!-- js component based work view code -->
<div class="work-view" style="visibility: hidden;">
	<div class="work-view-header">

		<!-- the left 3 dot menu icon -->
		<span class="wkv-header-item-left wkv-header-item-menu"><i id="menu.icon"
				class="wkv-header-menu-ctrl"
				onclick="WbApp.onViewAction(event, 'header.menu')" title="Menu"></i>
		</span>

		<!-- the popup menu -->
		<span id="header.menu" class="wkv-header-item-left wkv-header-menu-container">
			<div class="wkv-header-menu">
			</div>
		</span>

		<!-- the working indicator gif -->
		<span class="wkv-header-item-left">
			<img id="work.indicator" class="wkv-header-work-indicator" src="images/work-indicator-header-small.gif">
		</span>

		<!-- the title -->
		<span id="view.title" class="wkv-header-item-title">Unknown</span>

		<!-- the standard header icons -->
		<span class="wkv-header-item-right"><i id="pin.icon" class="wkv-header-action-ctrl"
				onclick="WbApp.onViewAction(event, 'pin')" title="Pin to keep view"></i></span>
		<span class="wkv-header-item-right"><i id="collapse.icon"
				class="wkv-header-action-ctrl"
				onclick="WbApp.onViewAction(event, 'collapse')" title="Collapse view"></i></span>
		<span class="wkv-header-item-right"><i id="close.icon" class="wkv-header-action-ctrl"
				onclick="WbApp.onViewAction(event, 'close')" title="Close View"></i></span>
	</div>

	<!-- the individual view working/content area -->
	<div id="work.view.workarea" class="work-view-body">
	</div>
</div>
<!-- work view end -->
`;