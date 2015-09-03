//
//  AppDelegate+notification.h
//
//  Created by Steven Livingstone (steven@livz.org) on 03 Sept 2015.
//  Based on original by Robert Easterday on 10/26/12.
//
//

#import "AppDelegate.h"

@interface AppDelegate (notification)
- (void)application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken;
- (void)application:(UIApplication *)application didFailToRegisterForRemoteNotificationsWithError:(NSError *)error;
- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo;
- (void)applicationDidBecomeActive:(UIApplication *)application;
- (void)application:(UIApplication *)application didRegisterUserNotificationSettings:(UIUserNotificationSettings *)notificationSettings;
- (void)application:(UIApplication *)application handleActionWithIdentifier:(NSString *)identifier forRemoteNotification:(NSDictionary *)userInfo completionHandler:(void(^)())completionHandler;
- (id) getCommandInstance:(NSString*)className;

@property (nonatomic, retain) NSDictionary  *launchNotification;

@end
