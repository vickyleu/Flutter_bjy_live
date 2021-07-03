#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#
Pod::Spec.new do |s|
  s.name             = 'flutter_live'
  s.version          = '0.0.1'
  s.summary          = 'A new Flutter plugin.'
  s.description      = <<-DESC
  A new Flutter plugin.
  DESC
  s.homepage         = 'http://example.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Company' => 'email@example.com' }
  s.source           = { :path => '.'}
  s.source_files = 'Classes/*{.m,.swift,.h}'
  s.public_header_files = 'Classes/*.{h}'

  s.resources = 'Classes/resource.bundle'
  s.dependency 'Flutter'
  s.dependency 'BaijiaYun/BJVideoPlayerUI', '2.11.12-tencent-professional'
  s.dependency 'BaijiaYun/BJPlaybackUI', '2.11.12-tencent-professional'
  s.dependency 'BaijiaYun/BJLiveUI', '2.11.12-tencent-professional'

  s.static_framework = true
  s.ios.deployment_target = '9.0'
  s.swift_version = "5"

  
end


