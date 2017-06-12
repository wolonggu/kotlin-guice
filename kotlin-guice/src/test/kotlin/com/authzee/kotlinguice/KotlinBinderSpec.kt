package com.authzee.kotlinguice

import com.authzee.kotlinguice.binder.annotatedWith
import com.authzee.kotlinguice.binder.to
import com.google.inject.Guice
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.google.inject.spi.ElementSource
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBe
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.util.concurrent.Callable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author John Leacox
 */
object KotlinBinderSpec : Spek({
    beforeEachTest {
        StaticInjectionObj.reset()
    }

    describe("KotlinBinder") {
        describe("#bindScope") {
            it("should bind a custom scope using a scope annotation type parameter") {
                val scope = TestScope()
                val injector = Guice.createInjector(object : KotlinModule() {
                    override fun configure() {
                        kotlinBinder.bindScope<TestScoped>(scope)
                        kotlinBinder.bind<A>().to<AImpl>().`in`<TestScoped>()
                    }
                })

                val a = injector.getInstance(A::class.java)
                a shouldBe injector.getInstance(A::class.java)

                scope.reset()

                a shouldNotBe injector.getInstance(A::class.java)
            }
        }

        describe("#bind") {
            it("should bind source using a type parameter") {
                val injector = Guice.createInjector(object : KotlinModule() {
                    override fun configure() {
                        kotlinBinder.bind<A>().to(AImpl::class.java)
                    }
                })

                val a = injector.getInstance(A::class.java)

                a.get() shouldEqual "Impl of A"
            }

            it("should bind a complex source using a type parameter") {
                val injector = Guice.createInjector(object : KotlinModule() {
                    override fun configure() {
                        kotlinBinder.bind<Callable<A>>().to(ACallable::class.java)
                    }
                })

                val a = injector.getInstance(Key.get(object : TypeLiteral<Callable<A>>() {}))
                a.call().get() shouldEqual "Impl of A"
            }

            it("should bind to a target using a type parameter") {
                val injector = Guice.createInjector(object : KotlinModule() {
                    override fun configure() {
                        kotlinBinder.bind<A>().to<AImpl>()
                    }
                })

                val a = injector.getInstance(A::class.java)

                a.get() shouldEqual "Impl of A"
            }

            it("should bind to a complex target using a type parameter") {
                val injector = Guice.createInjector(object : KotlinModule() {
                    override fun configure() {
                        kotlinBinder.bind<Callable<A>>().to<TCallable<A>>()
                    }
                })

                val callable = injector.getInstance(Key.get(object : TypeLiteral<Callable<A>>() {}))
                callable.call() shouldEqual null
            }

            it("should bind with an annotation using a type parameter") {
                val injector = Guice.createInjector(object : KotlinModule() {
                    override fun configure() {
                        kotlinBinder.bind<A>().to<B>()
                        kotlinBinder.bind<A>().annotatedWith<Annotated>().to<AImpl>()
                    }
                })

                val a = injector.getInstance(Key.get(A::class.java, Annotated::class.java))

                a.get() shouldEqual "Impl of A"
            }

            it("should bind to a provider using a type parameter") {
                val injector = Guice.createInjector(object : KotlinModule() {
                    override fun configure() {
                        kotlinBinder.bind<A>().toProvider<BProvider>()
                    }
                })

                val a = injector.getInstance(A::class.java)

                a shouldBeInstanceOf B::class.java
            }

            it("should bind to a complex provider using a type parameter") {
                val injector = Guice.createInjector(object : KotlinModule() {
                    override fun configure() {
                        kotlinBinder.bind<Iterable<A>>().toProvider<TProvider<List<A>>>()
                    }
                })

                val iterable = injector.getInstance(Key.get(object : TypeLiteral<Iterable<A>>() {}))
                iterable shouldEqual null
            }

            it("should bind in a scope") {
                val injector = Guice.createInjector(object : KotlinModule() {
                    override fun configure() {
                        kotlinBinder.bind<A>().to<AImpl>().`in`<Singleton>()
                    }
                })

                val a = injector.getInstance(A::class.java)
                a shouldBe injector.getInstance(A::class.java)
            }
        }

        describe("#bindConstant") {
            it("should bind to a target using a type parameter and annotation") {
                class ClassWithConstant @Inject constructor(@Annotated val constant: Class<Nothing>)

                val injector = Guice.createInjector(object : KotlinModule() {
                    override fun configure() {
                        kotlinBinder.bindConstant().annotatedWith<Annotated>().to<Iterator<*>>()
                    }
                })

                val classWithConstant = injector.getInstance(ClassWithConstant::class.java)
                classWithConstant.constant shouldEqual Iterator::class.java
            }
        }

        describe("#requestStaticInjection") {
            it("should inject static fields") {
                Guice.createInjector(object : KotlinModule() {
                    override fun configure() {
                        kotlinBinder.bind<String>().toInstance("Statically Injected")
                        kotlinBinder.requestStaticInjection<StaticInjectionObj>()
                    }
                })

                StaticInjectionObj.staticInjectionSite shouldEqual "Statically Injected"
            }

        }

        describe("#skipSource") {
            it("should do skip the class in the source trace") {
                class SkipSourceModule : KotlinModule() {
                    override fun configure() {
                        kotlinBinder.skipSource<SkipSourceModule>().bind<A>().to<AImpl>()
                    }
                }

                val outerModule = object : KotlinModule() {
                    override fun configure() {
                        install(SkipSourceModule())
                    }

                }

                val injector = Guice.createInjector(outerModule)

                val source = injector.getBinding(A::class.java).source as ElementSource
                val stackTraceElement = source.declaringSource as StackTraceElement

                outerModule::class.java.name shouldEqual stackTraceElement.className
            }
        }

        describe("#getProvider") {
            it("should get a provider") {
                Guice.createInjector(object : KotlinModule() {
                    override fun configure() {
                        kotlinBinder.bind<A>().to<AImpl>()
                        val provider = kotlinBinder.getProvider<A>()
                        provider.toString() shouldEqual "Provider<com.authzee.kotlinguice.A>"
                    }

                })
            }
        }

        describe("#getMembersInjector") {
            it("should inject member fields") {
                Guice.createInjector(object : KotlinModule() {
                    override fun configure() {
                        val membersInjector = kotlinBinder.getMembersInjector<AImpl>()
                        membersInjector.toString() shouldEqual
                                "MembersInjector<com.authzee.kotlinguice.AImpl>"
                    }
                })
            }
        }
    }
})
