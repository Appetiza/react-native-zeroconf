Object.defineProperty(exports,"__esModule",{value:true});var _createClass=function(){function defineProperties(target,props){for(var i=0;i<props.length;i++){var descriptor=props[i];descriptor.enumerable=descriptor.enumerable||false;descriptor.configurable=true;if("value"in descriptor)descriptor.writable=true;Object.defineProperty(target,descriptor.key,descriptor);}}return function(Constructor,protoProps,staticProps){if(protoProps)defineProperties(Constructor.prototype,protoProps);if(staticProps)defineProperties(Constructor,staticProps);return Constructor;};}();var _reactNative=require('react-native');
var _events=require('events');function _classCallCheck(instance,Constructor){if(!(instance instanceof Constructor)){throw new TypeError("Cannot call a class as a function");}}function _possibleConstructorReturn(self,call){if(!self){throw new ReferenceError("this hasn't been initialised - super() hasn't been called");}return call&&(typeof call==="object"||typeof call==="function")?call:self;}function _inherits(subClass,superClass){if(typeof superClass!=="function"&&superClass!==null){throw new TypeError("Super expression must either be null or a function, not "+typeof superClass);}subClass.prototype=Object.create(superClass&&superClass.prototype,{constructor:{value:subClass,enumerable:false,writable:true,configurable:true}});if(superClass)Object.setPrototypeOf?Object.setPrototypeOf(subClass,superClass):subClass.__proto__=superClass;}

var RNZeroconf=_reactNative.NativeModules.RNZeroconf;var

Zeroconf=function(_EventEmitter){_inherits(Zeroconf,_EventEmitter);

function Zeroconf(props){_classCallCheck(this,Zeroconf);var _this=_possibleConstructorReturn(this,(Zeroconf.__proto__||Object.getPrototypeOf(Zeroconf)).call(this,
props));

_this._services={};
_this._dListeners={};

_this.addDeviceListeners();return _this;
}_createClass(Zeroconf,[{key:'addDeviceListeners',value:function addDeviceListeners()




{var _this2=this;

if(Object.keys(this._dListeners).length){
return this.emit('error',new Error('RNZeroconf listeners already in place.'));
}

this._dListeners.start=_reactNative.DeviceEventEmitter.addListener('RNZeroconfStart',function(){return _this2.emit('start');});
this._dListeners.stop=_reactNative.DeviceEventEmitter.addListener('RNZeroconfStop',function(){return _this2.emit('stop');});
this._dListeners.error=_reactNative.DeviceEventEmitter.addListener('RNZeroconfError',function(err){return _this2.emit('error',err);});

this._dListeners.found=_reactNative.DeviceEventEmitter.addListener('RNZeroconfFound',function(service){
if(!service||!service.name){return;}var
name=service.name;

_this2._services[name]=service;
_this2.emit('found',name);
_this2.emit('update');
});

this._dListeners.remove=_reactNative.DeviceEventEmitter.addListener('RNZeroconfRemove',function(service){
if(!service||!service.name){return;}var
name=service.name;

delete _this2._services[name];

_this2.emit('remove',name);
_this2.emit('update');
});

this._dListeners.resolved=_reactNative.DeviceEventEmitter.addListener('RNZeroconfResolved',function(service){
if(!service||!service.name){return;}

_this2._services[service.name]=service;
_this2.emit('resolved',service);
_this2.emit('update');
});

}},{key:'removeDeviceListeners',value:function removeDeviceListeners()




{var _this3=this;
Object.keys(this._dListeners).forEach(function(name){return _this3._dListeners[name].remove();});
this._dListeners={};
}},{key:'getServices',value:function getServices()




{
return this._services;
}},{key:'scan',value:function scan()





{var type=arguments.length>0&&arguments[0]!==undefined?arguments[0]:'http';var protocol=arguments.length>1&&arguments[1]!==undefined?arguments[1]:'tcp';var domain=arguments.length>2&&arguments[2]!==undefined?arguments[2]:'local.';
this._services={};
this.emit('update');
RNZeroconf.scan(type,protocol,domain);
}},{key:'stop',value:function stop()




{
RNZeroconf.stop();
}}]);return Zeroconf;}(_events.EventEmitter);exports.default=Zeroconf;
