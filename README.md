# RetrofitDynamicBaseUrl
自定义gradle插件动态注入注解，优化动态替换retrofit的baseUrl

在工程目录下的build.gradle文件下添加依赖：
~~~
classpath 'org.linwg.dynamic:plugins:1.0.1'
~~~
在module目录下的build.gradle文件下添加插件：
~~~
apply plugin: 'org.linwg.dynamic'
~~~
插件可配置属性：
~~~
dynamicBaseUrlConfig{
    services 'ApiService'                                               //服务类名称，可选，类型-字符串数组
    serviceAnnotation 'org.linwg.retrofitdynamicbaseurl.ServiceHeader'  //注解于服务类上的注解类包名与类名，
    methodAnnotation 'retrofit2.http.Headers'                           //方法上的注解名称
    ignoreAnnotation 'org.linwg.retrofitdynamicbaseurl.DynamicIgnore'   //使用此注解，将使方法不会被动态注入Header
    ignoreJar true                                                      //是否忽略jar包注入
}
~~~
## 1.0.1
新增对module 为 lib 的支持
 ![img](https://github.com/linwg1988/RetrofitDynamicBaseUrl/blob/master/demo.gif) 
