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
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'



  s.static_framework = true
  s.resources = 'Classes/resource.bundle'
  s.dependency 'Flutter'


  s.dependency 'BJLiveUI'
  s.dependency 'BJPlaybackUI'

  # dev
  s.dependency 'BJLiveCore'

  s.dependency 'BJVideoPlayerCore'

  s.dependency "BJLiveBase"

  s.dependency 'SDWebImage/GIF'

s.dependency "SDWebImage/Core","5.0.0-beta"
  s.dependency 'BJPlayerManagerCore'




#   # 用于动态引入 Framework，避免冲突问题
#   s.script_phase = { :name => '[BJLiveCore] Embed Frameworks',\
#      :script => '${PODS_ROOT}/BJLiveCore/frameworks/EmbedFrameworks.sh',
#      :execution_position => :after_compile}
#   # 点播回放包括直播 SDK 需要加上
#   s.script_phase = { :name => '[BJLiveCore] Embed Frameworks',\
#    :script => '${PODS_ROOT}/BJLiveCore/frameworks/EmbedFrameworks.sh',
#    :execution_position => :after_compile}
#   # 用于清理动态引入的 Framework 用不到的架构，避免发布 AppStore 时发生错误，需要写在动态引入 Framework 的 script 之后
#   s.script_phase = { :name => '[BJLiveBase] Clear Archs From Frameworks',\
#    :script => '${PODS_ROOT}/BJLiveBase/script/ClearArchsFromFrameworks.sh "BJHLMediaPlayer.framework" "BJYIJKMediaFramework.framework"',
#    :execution_position => :after_compile}



  s.ios.deployment_target = '9.0'

 s.resource_bundles = {
        'BJVideoPlayerUI' => ['Classes/BJVideoPlayerUI/Assets/*.png']
      }

end

