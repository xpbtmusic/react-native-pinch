//
//  RNNativeFetch.m
//  medipass
//
//  Created by Paul Wong on 13/10/16.
//  Copyright © 2016 Localz. All rights reserved.
//

#import "RNPinch.h"
#import "RCTBridge.h"

@interface RNPinchException : NSException
@end
@implementation RNPinchException
@end

// private delegate for verifying certs
@interface NSURLSessionSSLPinningDelegate:NSObject <NSURLSessionDelegate>

- (id)initWithCertNames:(NSArray<NSString *> *)certNames;

@property (nonatomic, strong) NSArray<NSString *> *certNames;

@end

@implementation NSURLSessionSSLPinningDelegate

- (id)initWithCertNames:(NSArray<NSString *> *)certNames {
    if (self = [super init]) {
        _certNames = certNames;
    }
    return self;
}

- (NSArray *)pinnedCertificateData {
    NSMutableArray *localCertData = [NSMutableArray array];
    for (NSString* certName in self.certNames) {
        NSString *cerPath = [[NSBundle mainBundle] pathForResource:certName ofType:@"cer"];
        if (cerPath == nil) {
            @throw [[RNPinchException alloc]
                initWithName:@"CertificateError"
                reason:@"Can not load certicate given, check it's in the app resources."
                userInfo:nil];
        }
        [localCertData addObject:[NSData dataWithContentsOfFile:cerPath]];
    }

    NSMutableArray *pinnedCertificates = [NSMutableArray array];
    for (NSData *certificateData in localCertData) {
        [pinnedCertificates addObject:(__bridge_transfer id)SecCertificateCreateWithData(NULL, (__bridge CFDataRef)certificateData)];
    }
    return pinnedCertificates;
}

- (void)URLSession:(NSURLSession *)session didReceiveChallenge:(NSURLAuthenticationChallenge *)challenge completionHandler:(void (^)(NSURLSessionAuthChallengeDisposition disposition, NSURLCredential * _Nullable credential))completionHandler {

    if ([[[challenge protectionSpace] authenticationMethod] isEqualToString:NSURLAuthenticationMethodServerTrust]) {
        NSString *domain = challenge.protectionSpace.host;
        SecTrustRef serverTrust = [[challenge protectionSpace] serverTrust];

        NSArray *policies = @[(__bridge_transfer id)SecPolicyCreateSSL(true, (__bridge CFStringRef)domain)];

        SecTrustSetPolicies(serverTrust, (__bridge CFArrayRef)policies);
        // setup
        SecTrustSetAnchorCertificates(serverTrust, (__bridge CFArrayRef)self.pinnedCertificateData);
        SecTrustResultType result;

        // evaluate
        OSStatus errorCode = SecTrustEvaluate(serverTrust, &result);

        BOOL evaluatesAsTrusted = (result == kSecTrustResultUnspecified || result == kSecTrustResultProceed);
        if (errorCode == errSecSuccess && evaluatesAsTrusted) {
            NSURLCredential *credential = [NSURLCredential credentialForTrust:serverTrust];
            completionHandler(NSURLSessionAuthChallengeUseCredential, credential);
        } else {
            completionHandler(NSURLSessionAuthChallengeRejectProtectionSpace, NULL);
        }
    } else {
        completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, NULL);
    }
}

@end

@interface RNPinch()

@property (nonatomic, strong) NSURLSessionConfiguration *sessionConfig;

@end

@implementation RNPinch
RCT_EXPORT_MODULE();

- (instancetype)init
{
    self = [super init];
    if (self) {
        self.sessionConfig = [NSURLSessionConfiguration ephemeralSessionConfiguration];
        self.sessionConfig.HTTPCookieStorage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
    }
    return self;
}

RCT_EXPORT_METHOD(fetch:(NSString *)url obj:(NSDictionary *)obj callback:(RCTResponseSenderBlock)callback) {
    NSURL *u = [NSURL URLWithString:url];
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] initWithURL:u];

    NSURLSession *session;
    if (obj) {
        if (obj[@"method"]) {
            [request setHTTPMethod:obj[@"method"]];
        }
        if (obj[@"timeoutInterval"]) {
          [request setTimeoutInterval:[obj[@"timeoutInterval"] doubleValue] / 1000];
        }
        [request setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
        [request setValue:@"application/json" forHTTPHeaderField:@"Accept"];
        if (obj[@"headers"] && [obj[@"headers"] isKindOfClass:[NSDictionary class]]) {
            NSMutableDictionary *m = [obj[@"headers"] mutableCopy];
            for (NSString *key in m) {
                if([[key lowercaseString]isEqualToString:@"authorization"]){
                    m[key]=[@"Token " stringByAppendingString: m[key] ];
                    printf("%s -------------------------------\n",[m[key] UTF8String]);
                }
                if (![m[key] isKindOfClass:[NSString class]]) {
                    m[key] = [m[key] stringValue];
                }
            }
            [request setAllHTTPHeaderFields:m];
        }
        if (obj[@"body"]&&(!obj[@"upload"]||(obj[@"upload"]&&!obj[@"upload"][@"files"]))) {
            NSData *data = [obj[@"body"] dataUsingEncoding:NSUTF8StringEncoding];
            [request setHTTPBody:data];
        }
    }
    if(obj && obj[@"upload"]&&obj[@"upload"][@"files"]){
         NSMutableData *data = [NSMutableData data];
        NSString *boundary = multipartFormBoundary();
       NSString *name = [[NSString alloc] init];

        name=@"image";
        int count=-1;
          //upload params
        if (obj[@"body"]){
            //for (NSString *object in obj[@"body"]){
            NSData *jsonData = [obj[@"body"] dataUsingEncoding:NSUTF8StringEncoding];

             NSDictionary  *retDict = [NSJSONSerialization JSONObjectWithData:jsonData options:kNilOptions error:NULL];
            if(retDict){
                for(NSString *key in retDict){
                    NSLog(@"%@------0000-------%@----",key,retDict[key]);

                    //[data appendData:[@"\r\n" dataUsingEncoding:NSUTF8StringEncoding]];
                    NSString *bodyStr_ = [NSString stringWithFormat:@"\r\n--%@\r\n", boundary];
                    [data appendData:[bodyStr_ dataUsingEncoding:NSUTF8StringEncoding]];
                    NSString *bodyStr = [NSString stringWithFormat:@"Content-Disposition: form-data; name=\"%@\"",key];
                    [data appendData:[bodyStr dataUsingEncoding:NSUTF8StringEncoding]];
                    [data appendData:[@"\r\n" dataUsingEncoding:NSUTF8StringEncoding]];
                    [data appendData:[@"Content-Type:text/plain" dataUsingEncoding:NSUTF8StringEncoding]];
                    [data appendData:[@"\r\n" dataUsingEncoding:NSUTF8StringEncoding]];
                    [data appendData:[@"\r\n" dataUsingEncoding:NSUTF8StringEncoding]];
                    [data appendData:[retDict[key] dataUsingEncoding:NSUTF8StringEncoding]];

                    //[data appendData:[@"\r\n" dataUsingEncoding:NSUTF8StringEncoding]];
                }

            }


        }
       //upload file(s)
        for (NSString *filePath in obj[@"upload"][@"files"]){
            NSLog(@"%@------888-----------",filePath);
            count++;
            NSString *bodyStr = [NSString stringWithFormat:@"\r\n--%@\r\n", boundary];
            [data appendData:[bodyStr dataUsingEncoding:NSUTF8StringEncoding]];
            NSArray *pathStrings = [filePath componentsSeparatedByString:@"/"];
            int length=(int)pathStrings.count;

            NSString *fileName = pathStrings[length-1];
            NSLog(@"%@------999-----------",fileName);
            bodyStr = [NSString stringWithFormat:@"Content-Disposition: form-data; name=\"%@\"; filename=\"%@\" \r\n", name, fileName];
            [data appendData:[bodyStr dataUsingEncoding:NSUTF8StringEncoding]];
            [data appendData:[@"Content-Type: application/octet-stream\r\n\r\n" dataUsingEncoding:NSUTF8StringEncoding]];

            [data appendData:[NSData dataWithContentsOfFile:filePath]];

            [data appendData:[@"\r\n" dataUsingEncoding:NSUTF8StringEncoding]];
        }
        NSString *tailStr = [NSString stringWithFormat:@"--%@--\r\n", boundary];
        [data appendData:[tailStr dataUsingEncoding:NSUTF8StringEncoding]];


        request.HTTPBody = data;

        NSString *headerString = [NSString stringWithFormat:@"multipart/form-data; boundary=%@", boundary];
        [request setValue:headerString forHTTPHeaderField:@"Content-Type"];

    }
    //NSLog(@"%@-----------------",obj[@"upload"][@"files"][0]);

    if (obj && obj[@"sslPinning"] && obj[@"sslPinning"][@"cert"]) {
        NSURLSessionSSLPinningDelegate *delegate = [[NSURLSessionSSLPinningDelegate alloc] initWithCertNames:@[obj[@"sslPinning"][@"cert"]]];
        session = [NSURLSession sessionWithConfiguration:self.sessionConfig delegate:delegate delegateQueue:[NSOperationQueue mainQueue]];
    } else if (obj && obj[@"sslPinning"] && obj[@"sslPinning"][@"certs"]) {
        // load all certs
        NSURLSessionSSLPinningDelegate *delegate = [[NSURLSessionSSLPinningDelegate alloc] initWithCertNames:obj[@"sslPinning"][@"certs"]];
        session = [NSURLSession sessionWithConfiguration:self.sessionConfig delegate:delegate delegateQueue:[NSOperationQueue mainQueue]];
    } else {
        session = [NSURLSession sessionWithConfiguration:self.sessionConfig];
    }

    __block NSURLSessionDataTask *dataTask = [session dataTaskWithRequest:request completionHandler:^(NSData * _Nullable data, NSURLResponse * _Nullable response, NSError * _Nullable error) {
        if (!error) {
            dispatch_async(dispatch_get_main_queue(), ^{
                NSHTTPURLResponse *httpResp = (NSHTTPURLResponse*) response;
                NSInteger statusCode = httpResp.statusCode;
                NSString *bodyString = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
                NSString *statusText = [NSHTTPURLResponse localizedStringForStatusCode:httpResp.statusCode];

                NSDictionary *res = @{
                                      @"status": @(statusCode),
                                      @"headers": httpResp.allHeaderFields,
                                      @"bodyString": bodyString,
                                      @"statusText": statusText
                                      };
                callback(@[[NSNull null], res]);
            });
        } else {
            dispatch_async(dispatch_get_main_queue(), ^{
                callback(@[@{@"message":error.localizedDescription}, [NSNull null]]);
            });
        }
    }];

    [dataTask resume];
}
static NSString * multipartFormBoundary() {
    return [NSString stringWithFormat:@"Boundary+%08X%08X", arc4random(), arc4random()];
}
@end