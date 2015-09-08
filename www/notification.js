/* global cordova:false */

  /*!
   * Module dependencies.
   */

  var exec = cordova.require('cordova/exec');

  /**
   * NotificationHub constructor.
   * Initializes a new instance of the NotificationHub class.
   * http://msdn.microsoft.com/en-us/library/microsoft.windowsazure.messaging.notificationhub.notificationhub.aspx
   *
   * @param {string} notificationHubPath The notification hub path (name).
   * @param {string} connectionString The connection string.
   * @param {string} options Platform specific additional parameters (optional).
   * @return {NotificationHub} instance that can be monitored and cancelled.
   */
  var NotificationHub = function(notificationHubPath, connectionString, options) {
    this._handlers = {
      'registration': [],
      'notification': [],
      'error': []
    };

    console.log('Options: ' + JSON.stringify(options));

    // require options parameter
    if (typeof options === 'undefined') {
      throw new Error('The options argument is required.');
    }

    // store the options to this object instance
    this.options = options;

    // triggered on registration and notification
    var that = this;
    var success = function(result) {
      if (result && typeof result.registrationId !== 'undefined') {
        that.emit('registration', result);
      } else if (result && typeof result.callback !== 'undefined') {
        var executeFunctionByName = function(functionName, context /*, args */) {
          var args = Array.prototype.slice.call(arguments, 2);
          var namespaces = functionName.split(".");
          var func = namespaces.pop();
          for (var i = 0; i < namespaces.length; i++) {
            context = context[namespaces[i]];
          }
          return context[func].apply(context, args);
        }

        executeFunctionByName(result.callback, window, result);
      } else if (result) {
        that.emit('notification', result);
      }
    };

    // triggered on error
    var fail = function(msg) {
      var e = (typeof msg === 'string') ? new Error(msg) : msg;
      that.emit('error', e);
    };

    // wait at least one process tick to allow event subscriptions
    setTimeout(function() {
      exec(success, fail, 'NotificationHub', 'init', [notificationHubPath, connectionString, options]);
    }, 10);
  };

  /**
   * Unregister from push notifications
   */

  NotificationHub.prototype.unregister = function(successCallback, errorCallback, notificationHubPath, connectionString, options) {
    if (errorCallback == null) { errorCallback = function() {}}

    if (typeof errorCallback != "function")  {
      console.log("NotificationHub.unregister failure: failure parameter not a function");
      return
    }

    if (typeof successCallback != "function") {
      console.log("NotificationHub.unregister failure: success callback parameter must be a function");
      return
    }

    exec(successCallback, errorCallback, "NotificationHub", "unregister", [notificationHubPath, connectionString, options]);
  };

  /**
   * Call this to set the application icon badge
   */

  NotificationHub.prototype.setApplicationIconBadgeNumber = function(successCallback, errorCallback, badge) {
    if (errorCallback == null) { errorCallback = function() {}}

    if (typeof errorCallback != "function")  {
      console.log("NotificationHub.setApplicationIconBadgeNumber failure: failure parameter not a function");
      return
    }

    if (typeof successCallback != "function") {
      console.log("NotificationHub.setApplicationIconBadgeNumber failure: success callback parameter must be a function");
      return
    }

    exec(successCallback, errorCallback, "NotificationHub", "setApplicationIconBadgeNumber", [{badge: badge}]);
  };

  /**
   * Listen for an event.
   *
   * The following events are supported:
   *
   *   - registration
   *   - notification
   *   - error
   *
   * @param {String} eventName to subscribe to.
   * @param {Function} callback triggered on the event.
   */

  NotificationHub.prototype.on = function(eventName, callback) {
    if (this._handlers.hasOwnProperty(eventName)) {
      this._handlers[eventName].push(callback);
    }
  };

  /**
   * Emit an event.
   *
   * This is intended for internal use only.
   *
   * @param {String} eventName is the event to trigger.
   * @param {*} all arguments are passed to the event listeners.
   *
   * @return {Boolean} is true when the event is triggered otherwise false.
   */

  NotificationHub.prototype.emit = function() {
    var args = Array.prototype.slice.call(arguments);
    var eventName = args.shift();

    if (!this._handlers.hasOwnProperty(eventName)) {
      return false;
    }

    for (var i = 0, length = this._handlers[eventName].length; i < length; i++) {
      this._handlers[eventName][i].apply(undefined,args);
    }

    return true;
  };

  /*!
   * NotificationHub Plugin.
   */

  module.exports = {
    /**
     * Register for NotificationHub.
     *
     * This method will instantiate a new copy of the NotificationHub object
     * and start the registration process.
     *
     * @param {string} notificationHubPath The notification hub path (name).
     * @param {string} connectionString The connection string.
     * @param {string} options Platform specific additional parameters (optional).
     * @return {NotificationHub} instance that can be monitored and cancelled.
     */

    init: function(notificationHubPath, connectionString, options) {
      return new NotificationHub(notificationHubPath, connectionString, options);
    },

    /**
     * NotificationHub Object.
     *
     * Expose the NotificationHub object for direct use
     * and testing. Typically, you should use the
     * .init helper method.
     */

    NotificationHub: NotificationHub
  };
