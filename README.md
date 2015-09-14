#phonegap-plugin-notificationhub

A plugin based on the PhoneGap Push plugin adapted to work with 
Azure Notification Hubs.

To see a working exam please check:
https://github.com/ParamountVentures/AzureNotificationHubsCrossPlatformDemo

#Windows
In the current version of Cordova (5.2.0) the Identity within the AppManifest is NOT updated with the correct package information after a build. Hence to use push notifications, you MUST update the Identity element within your package.phone.appxmanifest to be the that of your app registered in the Windows Store:

e.g. 
<Identity Name="MyPackage.Name" Publisher="CN=0000000-1111-2222-3333-444444444" Version="1.0.0.0" /> 

#Overview
This plugin uses a combination of the official PushPlugin (for iOS and Android) and the Microsoft Open Technologies Notification Hub plugin for Windows. You need to check for the device and use the appropriate script as follows:

if (device.platform == "windows") {

            // TODO: Cordova has been loaded. Perform any initialization that requires Cordova here.
            var connectionString = "STRING";
            var notificationHubPath = "STRING";

            var hub = new WindowsAzure.Messaging.NotificationHub(notificationHubPath, connectionString);

            hub.registerApplicationAsync().then(function (result) {
                console.log("Registration successful: " + result.registrationId);
            });

            hub.onPushNotificationReceived = function (msg) {
                console.log('onPushNotificationReceived:' + JSON.stringify(msg));
            };
        } else {

            try {
                var hub = NotificationHub.init(notificationHubPath,
                  connectionString,
                  {
                      "android": {
                          "senderID": "0000000"
                      },
                      "ios": {}
                  });

                hub.on('registration', function (data) {
                    console.log("registration event");
                    console.log(JSON.stringify(data));
                    console.log(data.registrationId);
                });

                hub.on('notification', function (data) {
                    console.log("notification event");
                    console.log(JSON.stringify(data));
                });

                hub.on('error', function (data) {
                    console.log("notification error");
                    console.log(JSON.stringify(data));
                });

            } catch (e) {
                console.log(e);
            }
        }

