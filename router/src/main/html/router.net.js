

// ****************************************************************
// Helpers

function objectEquals(x, y) {
    'use strict';

    if (x === null || x === undefined || y === null || y === undefined) { return x === y; }
    // after this just checking type of one would be enough
    if (x.constructor !== y.constructor) { return false; }
    // if they are functions, they should exactly refer to same one (because of closures)
    if (x instanceof Function) { return x === y; }
    // if they are regexps, they should exactly refer to same one (it is hard to better equality check on current ES)
    if (x instanceof RegExp) { return x === y; }
    if (x === y || x.valueOf() === y.valueOf()) { return true; }
    if (Array.isArray(x) && x.length !== y.length) { return false; }

    // if they are dates, they must had equal valueOf
    if (x instanceof Date) { return false; }

    // if they are strictly equal, they both need to be object at least
    if (!(x instanceof Object)) { return false; }
    if (!(y instanceof Object)) { return false; }

    // recursive object equality check
    var p = Object.keys(x);
    return Object.keys(y).every(function (i) { return p.indexOf(i) !== -1; }) &&
        p.every(function (i) { return objectEquals(x[i], y[i]); });
}



// ****************************************************************
// Packet & Protocol

class Packet {
  constructor(name, data) {
    this.name = name;
    this.data = data;
  }
  static curry(name, data) {
    if(name === undefined) {
      throw "undefined name";
    }
    if(data === undefined) {
      return function(data) {
        return new Packet(name, data);
      }
    } else {
      return function() {
        return new Packet(name, data);
      }
    }
  }
}


class Protocol {
  add(packet) {
    if(typeof packet == 'string') {
      if(packet == "add"){
        throw "invalid packet name";
      }
      this[packet] = Packet.curry(packet);
    } else {
      throw "invalid register packet";
    }
  }
}


class ProtocolHandler {
  constructor(protocol) {
   this.protocol = protocol;
    this.handlers = {};
    var self = this;
    Object.keys(protocol).forEach(function(k) {
      self.registerHandler(k, function(name, data){});
    });
  }
  registerAll(map) {
    var self = this;
    Object.keys(map).forEach(function(k) {
      if(typeof map[k] === 'function') {
        self.registerHandler(k, map[k]);
      }     
    });
  }
  registerHandler(name, handler) {
    if(typeof handler !== 'function') { throw "handler for " + name + " not a function"; }
    if(!(name in this.protocol)) {     
      //throw "packet " + name + " not in protocol"; 
      // auto register
      this.protocol.add(name);
    }    
    this.handlers[name] = handler;
  }
  handle(msg) {
    if(typeof msg.name != 'string') {
      throw "Malformed packet!";
    }
    if(msg.name in this.handlers) {
      this.handlers[msg.name](msg.data, msg.name);
    } else {
      throw "Handler for " + packet.name + " not found!";
    }
  }
}


// ****************************************************************
// Connection

var ReadyState = {
  CONNECTING: 0,
  OPEN: 1,
  CLOSING: 2,
  CLOSED: 3
}
Object.freeze(ReadyState);

class Connection {
  constructor(url, handler) {
    if(!url) { throw "no url"; }
    if(typeof handler.handle !== 'function') { throw "handler has not a handle function"; }
    this.url = url;
    this.handler = handler;
    this.socket = {readyState: ReadyState.CLOSED};
  }  
  state() { return this.socket.readyState; }
  connect() {
    this._connect();
  }
  _connect() {
    if(this.socket.readyState == ReadyState.CLOSED) {
      var self = this;
      var socket = new WebSocket(this.url);
      this.socket = socket;
      
      try {
        self._readyStateChanged(socket);          
      } catch (err) {
        socket.close();
        console.error(err);
        return;
      }
      
      socket.onopen = function(evt) {
        if(self.socket != socket) {
          socket.close();
          return;
        }
        try {
          self._readyStateChanged(socket);
          self.onOpen(evt, socket);
        } catch (err) {
          socket.close();
          console.error(err);
          return;
        }
      };
      socket.onmessage = function(evt) {
        self.onMessage(evt, socket);
      };
      socket.onclose = function(evt) {
        self._readyStateChanged(socket);
        self.onClose(evt, socket);
      };     
      socket.onerror = function(evt) {
        try {
          self._readyStateChanged(socket);
          self.onError(evt, socket);
        } finally {
          socket.close();
        }
      };
    }
  }
  disconnect() {
    if(typeof this.socket.close === 'function') {
      this.socket.close();
    }
  }
  onOpen(evt, socket) {}
  onMessage(evt, socket) {
    this.handler.handle(JSON.parse(evt.data));
  }
  onClose(evt, socket) {}
  onError(evt, socket) {}
  send(data){
    if(this.socket.readyState == ReadyState.OPEN) {
      if(data instanceof Packet) {
        this.socket.send(JSON.stringify(data));
      } else {
        this.socket.send(data);
      }
      return true;
    }
    return false;
  }
  _readyStateChanged(socket) {
    if(this.socket == socket) {
      this.onReadyStateChange(socket.readyState);
    }
  }
  onReadyStateChange(state) {}
}


// ****************************************************************
// WebProtocol with Services

var NetProtocol = new Protocol();
NetProtocol.add("subscribe");
NetProtocol.add("unsubscribe");

class Service {
  constructor(name, packets) {
   if(!name || name.length == 0 || typeof name !== 'string') {
      throw "name required";
    }
    this.name = name;
    this.handlers = {};
    var self = this;
    Object.keys(packets).forEach(function (k) {
      if(typeof packets[k] === 'function'){
        self.handlers[k] = packets[k];
      } else {
        throw "malformed packet handler map";
      }
    });
  }
  subscribe(connection) {
    connection.send(new NetProtocol.subscribe({"service": this.name}));
  }
  unsubscribe(connection) {
    connection.send(new NetProtocol.unsubscribe({"service": this.name}));
  }
}

