package com.daiyc.extension

import com.daiyc.extension.core.ExtensionRegistry
import com.daiyc.extension.example.Arguments
import com.daiyc.extension.example.FooStrategy
import com.daiyc.extension.example.BarStrategy
import com.daiyc.extension.example.Strategy
import spock.lang.Specification

/**
 * @author daiyc
 * @date 2024/7/20
 */
class LoadSpec extends Specification {
    def '测试加载'() {
        when:
        def registry = new ExtensionRegistry()
        registry.register(FooStrategy)
        registry.register(BarStrategy)

        def extensionLoader = registry.getExtensionLoader(Strategy)
        def extension = extensionLoader.getExtension()
        def fooArgs = new Arguments(name: "foo", number: 2)
        def res = extension.show(fooArgs)

        then:

        extension.getType("foo") == 'foo'
        res == 'foo: 2'
    }
}
