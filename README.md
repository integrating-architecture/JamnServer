# JamnServer
### Just Another Micro Node Server

Jamn is an **experimental**, lightweight **Java MicroServer** designed for independence, simplicity and easy customization.
The purpose is textdata based application communication e.g. with JSON/XML/HTML etc. based on a rudimentary HTTP compatibility.

The Server implementation has NO dependencies to any APIs or Libraries. It uses only standard Java SE functionality and is <a href="/org.isa.ipc.JamnServer/src/main/java/org/isa/ipc/JamnServer.java">implemented in ONE tiny class file.</a>

The basic design is a layered separation of socket, protocol and content, so you can easily adapt anything to your own needs.

E.g. combined with a small Content-Provider implementation like <a href="/org.isa.ipc.JamnWebServiceProvider">JamnWebServiceProvider</a>, Jamn can serve as a basis for lightweight (two classes) and straightforward REST-like Web APIs as you e.g. know from JavaScript.

**NOTE**: Although the current server supports http in a basic form, **it is NOT intended to be a real HTTP/Web Server and it is NOT suitable for such production purposes**. But it is quick and easy to use e.g. for tooling, testing or concept experiments  - cause no infrastructure and no external components are required.

<a href="/org.isa.ipc.JamnServer/src/test/java/org/isa/ipc/JamnServerBasicTest.java">A basic usage example</a>

<br />

## Disclamer  
Affects all source and binary code from
    https://github.com/integrating-architecture/JamnServer.git
	
Use and redistribution in source and binary forms,
with or without modification, are permitted WITHOUT restriction of any kind.  

THIS SOFTWARE IS PROVIDED BY THE AUTHORS, COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR, COPYRIGHT HOLDER OR CONTRIBUTOR
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, 
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

