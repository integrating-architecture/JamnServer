/* Authored by iqbserve.de */

import { NL, newSimpleId, fileUtil } from '../jsmod/tools.mjs';
import { WsoCommonMessage, CommandDef } from '../jsmod/data-classes.mjs';
import { WorkView, ViewBuilder, IconElement } from '../jsmod/view-classes.mjs';

/**
 * A general View class for commands.
 * An example of 
 *  - using web socket communication to execute server side "commands"
 *  - using dynamically integrated html source code
 *    using standard js/dom functionality
 *  - using a builder to create a ui
 * 
 * Commands are either server side JavaScripts or java classes.
 * 
 * The client side command definitions are located in:
 *  - sidebar-content.mjs
 * The web socket counterpart on the server-side is:
 *  - org.isa.jps.comp.DefaultWebSocketMessageProcessor
 * The server side scripts are located in subdir
 *  - "scripts" for JS / and "extensions" for java
 * 
 * Creating dynamic content and components
 * Jamn supports 3 approaches and their combinations:
 * - static file content
 * - server side dynamically generated and/or enriched content from any source  
 * - client side dynamically loaded, manipulated, created content and components
 */
class CommandView extends WorkView {
	//websocket communication ref id
	wsoRefId;

	commandDef = new CommandDef();
	commandName = "";
	attachments = new Map();
	namedArgs = { none: "" };

	//input element for file dialog
	fileInput = null;

	//member objects to collect ui elements and ui objects from the builder
	elem = {};
	uiobj = {};

	constructor(id) {
		super(id, "");
		//use this js component internal html view source code
		this.viewSource.html = viewHtml;
	}

	onInstallation(installKey, installData, viewManager) {
		super.onInstallation(installKey, installData, viewManager);
		this.id = installKey;
		if (installData instanceof CommandDef) {
			this.commandDef = installData;
			this.commandName = this.commandDef.command + " " + this.commandDef.script;
		}
	}

	initialize() {
		super.initialize();
		this.setTitle(this.commandDef.title);

		//just some demo data
		this.namedArgs = { help: "-h", testfile: "-file=test-data.json", cdata: '<![CDATA[ {"name":"HelloFunction", "args":["John Doe"]} ]]>' };

		this.headerMenu.addItem("Clear Output", (evt) => {
			this.clearOutput();
		}, { separator: "top" });
		this.headerMenu.addItem("Clear View", (evt) => {
			this.clearAll();
		});

		this.fileInput = fileUtil.createFileInputElement("text/*, .json, .txt", (evt) => {
			let [file] = this.fileInput.files;
			this.fileInput.value = "";
			this.addAttachment(file);
		});

		this.createUI();
		this.createWsoConnection();

		this.isInitialized = true;
		this.setVisible(true);
	}

	/**
	 * <pre>
	 * build the ui using
	 * - an included viewHtml template
	 * - and a dom element builder 
	 * </pre>
	 */
	createUI() {
		let builder = new ViewBuilder();
		//set the objects to hold all control dom elements with a varid
		builder.setElementCollection(this.elem);
		// and other things like e.g. datalists or "data-bind" infos
		builder.setObjectCollection(this.uiobj);

		//define some style defaults
		builder.defaultStyles.comp = { "margin-bottom": "0" };
		builder.defaultStyles.label = { "width": "80px" };
		let hgap = "20px";

		//create the control container
		let compSet = builder.newFieldset({ styleProps: { "margin-top": "10px", "gap": "10px" } });
		this.viewWorkarea.prepend(compSet);

		//create the controls
		builder.newViewComp()
			.addLabelButton({ text: "Command:" }, { varid: "pbRun", icon: "run", text: this.commandName, title: "Run command" }, (target) => {
				target.button.onclick = (evt) => {
					this.runCommand();
				};
			})
			.appendTo(compSet);

		//arguments choice + demo data
		let namedArgsList = Object.getOwnPropertyNames(this.namedArgs);
		builder.newViewComp()
			.style({ "align-items": "flex-start" })
			.addLabelTextArea({ text: "Args:" }, {
				varid: "taArgs",
				styleProps: { "width": "400px", "min-width": "400px", "height": "45px", "min-height": "45px", "text-align": "left" },
				attribProps: {
					title: (this.commandDef.options.args ? "Command arguments: -h for help" : "<no args>") + "\nStructured text like e.g. JSON must be wrapped in a <![CDATA[ structured text ]]> tag.",
					placeholder: (this.commandDef.options.args ? " -h + Enter for help" : "<no args>"),
					disabled: !this.commandDef.options.args
				}
			}, (target) => {
				target.textarea.onkeydown = (evt) => {
					if (this.commandDef.options.args) {
						if (evt.keyCode == 13 && evt.currentTarget.value.trim() === "-h") {
							this.runCommand();
						}
					}
				}
			})
			.addContainer({ clazzes: "wkv-col-container", styleProps: { "margin-left": hgap } }, (target) => {
				let container = target.container;

				target.comp.addTextField({
					parentCtrl: container, varid: "tfNamedArgs", datalist: namedArgsList,
					styleProps: { "width": "200px" }, attribProps: { placeholder: "named args", "data-bind": "namedArgs" }
				});

				target.comp.addContainer({ clazzes: "wkv-row-container", parentCtrl: container, styleProps: { gap: "20px", "align-self": "flex-end", "margin-top": "5px" } }, (target) => {
					let iconBar = target.container;

					target.comp.addActionIcon({ parentCtrl: iconBar, varid: "icoDeleteNamedArgs", iconName: "trash", title: "Delete current named arg" });
					target.comp.addContainer({ parentCtrl: iconBar, styleProps: { height: "20px", "border-right": "1px solid var(--border-gray)" } })
					target.comp.addActionIcon({ parentCtrl: iconBar, varid: "icoSaveNamedArgs", iconName: "save", title: "Save current named args" });
					target.comp.addActionIcon({ parentCtrl: iconBar, varid: "icoClearArgChoice", iconName: "eraser", title: "Clear args and choice", styleProps: { "margin-left": "20px", "margin-right": "5px" } })
				})
			})
			.appendTo(compSet);

		//attachment list
		builder.newViewComp()
			.style({ "align-items": "flex-start" })
			.addLabel({ text: "Attachments:" })
			.addContainer({ clazzes: "wkv-col-container" }, (target) => {
				let container = target.container;

				target.comp.addContainer({ clazzes: "wkv-row-container", parentCtrl: container, styleProps: { gap: "20px", "align-self": "flex-start" } }, (target) => {
					let iconBar = target.container;

					target.comp.addActionIcon({ parentCtrl: iconBar, varid: "icoRemoveAllAttachments", iconName: "trash", title: "Remove all Attachments" });
					target.comp.addActionIcon({ parentCtrl: iconBar, varid: "icoAddAttachment", iconName: "plusNew", title: "Add Attachment" });
				});

				target.comp.addList({
					parentCtrl: container, varid: "lstAttachments",
					styleProps: { "min-width": "385px", "min-height": "20px", "padding": "10px" }
				});

			})
			.appendTo(compSet);

		builder.newViewComp().addSeparator({ styleProps: { width: "100%" } }).appendTo(compSet);

		builder.newViewComp()
			.style({ "align-items": "flex-start" })
			.addContainer({ clazzes: "wkv-col-container", styleProps: { "align-items": "center", "gap": "15px" } }, (target) => {
				let labelBar = target.container;

				target.comp.addLabelTextArea(
					{ text: "Output:", parentCtrl: labelBar },
					{ varid: "taOutput", disabled: true, clazzes: ["wkv-output-textarea-ctrl"], styleProps: { width: "626px", "min-width": "626px" } })
					.addActionIcon({ parentCtrl: labelBar, varid: "icoOutputSave", iconName: "save", title: "Save current output to a file" }, (target) => {
						target.icon.onclick = () => {
							this.saveOutput();
						}
					})
					.addActionIcon({ parentCtrl: labelBar, varid: "icoOutputToClipboard", iconName: "clipboardAdd", title: "Copy current output to clipboard" }, (target) => {
						target.icon.onclick = () => {
							this.copyOutputToClipboard();
						}
					})
					.addActionIcon({ parentCtrl: labelBar, varid: "icoOutputDelete", iconName: "trash", title: "Delete current output" }, (target) => {
						target.icon.onclick = () => {
							this.clearOutput();
						}
					});
			})
			.appendTo(compSet);

		this.elem.tfNamedArgs.addEventListener('input', (evt) => {
			let key = evt.currentTarget.value;
			this.setArgsSelection(key);
		});
		this.elem.icoClearArgChoice.onclick = () => {
			this.clearArgChoice();
		};
		this.elem.icoDeleteNamedArgs.onclick = () => {
			this.deleteArgChoice();
		};
		this.elem.icoSaveNamedArgs.onclick = () => {
			this.saveArgChoice();
		};
		this.elem.icoAddAttachment.onclick = () => {
			this.fileInput.click();
		};
		this.elem.icoRemoveAllAttachments.onclick = () => {
			this.removeAllAttachments();
		}
	}

	createWsoConnection() {
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
					this.addOutputLine(wsoMsg.bodydata);
				}
			} else if (wsoMsg.hasStatusError && wsoMsg.error.includes("connection")) {
				this.addOutputLine(NL + wsoMsg.error);
				this.setRunning(false);
			} else if (wsoMsg.hasStatusError && wsoMsg.hasReference("server.global")) {
				this.addOutputLine(NL + "WebSocket Error [" + wsoMsg.error + "] the central connection was closed.");
				this.setRunning(false);
			}
		});
	}

	clearAll() {
		this.clearArgChoice();
		this.clearOutput();
		this.removeAllAttachments();

		//resize elements
		this.elem.taArgs.style.width = "0px";
		this.elem.taArgs.style.height = "0px";
		this.elem.taOutput.style.width = "0px";
		this.elem.taOutput.style.height = "0px";
	}

	setRunning(flag) {
		super.setRunning(flag);
		this.elem.pbRun.disabled = flag;
	}

	runCommand() {
		let wsoMsg = new WsoCommonMessage(this.wsoRefId);
		wsoMsg.command = this.commandDef.command;
		wsoMsg.functionModule = this.commandDef.script;
		wsoMsg.argsSrc = this.elem.taArgs.value.trim();

		if (this.attachments.size > 0) {
			this.attachments.forEach(function (value, key) {
				wsoMsg.addAttachment(value.name, value.data);
			})
		}

		this.clearOutput();
		WbApp.sendWsoMessage(wsoMsg, () => {
			this.setRunning(true);
		});
	}

	addOutputLine(line) {
		this.elem.taOutput.value += line + NL;
		this.elem.taOutput.scrollTop = this.elem.taOutput.scrollHeight;
	}

	setArgsSelection(key) {
		if (this.commandDef.options.args && this.namedArgs[key]) {
			this.elem.taArgs.value = this.namedArgs[key];
		}
	}

	clearArgChoice() {
		this.elem.taArgs.value = "";
		this.elem.tfNamedArgs.value = "";
	}

	getDataListObjFor(name) {
		return this.uiobj[this.elem[name].list.id];
	}

	saveArgChoice() {
		let key = this.elem.tfNamedArgs.value.trim();
		if (key != "") {
			this.namedArgs[key] = this.elem.taArgs.value.trim();
			let datalist = this.getDataListObjFor("tfNamedArgs");
			datalist.addOption(key);
		}
	}

	deleteArgChoice() {
		let key = this.elem.tfNamedArgs.value.trim();

		if (this.namedArgs[key]) {
			WbApp.confirm({
				message: `<b>Delete entry</b><br>Do you want to delete <b>[${key}]</b> from arg choice?`
			}, (val) => {
				if (val) {
					delete this.namedArgs[key];
					let dataListObj = this.getDataListObjFor("tfNamedArgs");
					dataListObj.removeOption(key);
					this.clearArgChoice();
				}
			});
		}
	}

	saveOutput() {
		let fileName = "output_" + (this.commandDef.command + "_" + this.commandDef.script).replaceAll("/", "_") + ".txt";
		this.saveToFile(fileName, this.elem.taOutput.value.trim());
	}

	copyOutputToClipboard() {
		this.copyToClipboard(this.elem.taOutput.value.trim());
	}

	clearOutput() {
		if (!this.isRunning) {
			let lastValue = this.elem.taOutput.value;
			this.elem.taOutput.value = "";
			return lastValue;
		}
		return "";
	}

	addAttachment(file) {
		if (file && !this.attachments.has(file.name)) {
			let attachment = new Attachment(file.name);
			let reader = new FileReader();
			reader.onload = (e) => {
				attachment.data = e.target.result;
			};
			reader.readAsText(file);

			this.attachments.set(file.name, attachment);
			this.addAttachmentToList(attachment);
		}
	}

	addAttachmentToList(attachment) {
		let item = document.createElement("li");
		item.classList.add("indexed");
		let iconClass = IconElement.iconDef("xRemove")[0];
		let html = `<span class='${iconClass} wkv-listitem-ctrl' title='Remove Attachment' style='margin-right: 20px;'></span> <span>${attachment.name}</span>`;
		item.innerHTML = html;

		this.elem.lstAttachments.appendChild(item);
		item.firstChild.onclick = (evt) => {
			let name = evt.target.parentElement.lastChild.textContent;
			this.removeAttachmentFromList(name, item);
		}
	}

	removeAttachmentFromList(name, item) {
		this.elem.lstAttachments.removeChild(item);
		this.attachments.delete(name);
	}

	removeAllAttachments() {
		this.attachments = new Map();
		let list = this.elem.lstAttachments;
		while (list.firstChild) list.removeChild(list.firstChild);
	}

}

//export this view component as instances
//because command views are individual wso message receiver
export function getView() {

	//use the html code from this js module
	return new CommandView("commandView");

	//alternatively load html code from a file
	//return new CommandView("commandView", "/jsmod/html-components/work-view.html");
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

class Attachment {
	name;
	data;
	constructor(name) {
		this.name = name;
	}
}