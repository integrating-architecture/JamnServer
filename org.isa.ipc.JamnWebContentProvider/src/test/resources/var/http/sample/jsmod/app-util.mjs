/*jamn.web.template*/

export const JamnServerInfo = {
	"name" : "JamnServer",
	"version" : "${server.version}",
	"description" : "Just another micro node Server"
}

export function setElementHTML (id, html){
	document.getElementById(id).innerHTML = html;
}
