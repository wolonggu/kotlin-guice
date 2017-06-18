package com.authzee.kotlinguice

import com.authzee.kotlinguice.binder.KotlinAnnotatedBindingBuilder
import com.google.inject.AbstractModule
import com.google.inject.MembersInjector
import com.google.inject.Provider
import com.google.inject.Scope

/**
 * An extension of [AbstractModule] that enhances the binding DSL to allow binding using reified
 * type parameters.
 *
 * By using this class instead of [AbstractModule] you can replace:
 * ```
 * class MyModule : AbstractModule() {
 *     override fun configure() {
 *         bind(Service::class.java).to(ServiceImpl::class.java).`in`(Singleton::class.java)
 *         bind(object : TypeLiteral<PaymentService<CreditCard>>() {})
 *                 .to(CreditCardPaymentService::class.java)
 *     }
 * }
 * ```
 * with
 * ```
 * class MyModule : KotlinModule() {
 *     override fun configure() {
 *         bind<Service>().to<ServiceImpl>().`in`<Singleton>()
 *         bind<PaymentService<CreditCard>>().to<CreditCardPaymentService>()
 *     }
 * }
 * ```
 *
 * @see KotlinBinder
 * @see AbstractModule
 * @author John Leacox
 * @since 1.0
 */
abstract class KotlinModule : AbstractModule() {
    /** Gets direct access to the underlying [KotlinBinder]. */
    protected val kotlinBinder: KotlinBinder by lazy {
        KotlinBinder(binder().skipSources(KotlinBinder::class.java))
    }

    /** @see KotlinBinder.bindScope */
    protected inline fun <reified TAnn : Annotation> bindScope(scope: Scope) {
        kotlinBinder.bindScope<TAnn>(scope)
    }

    /** @see KotlinBinder.bind */
    protected inline fun <reified T> bind(): KotlinAnnotatedBindingBuilder<T> {
        return kotlinBinder.bind<T>()
    }

    /** @see KotlinBinder.requestStaticInjection */
    protected inline fun <reified T> requestStaticInjection() {
        kotlinBinder.requestStaticInjection<T>()
    }

    /** @see AbstractModule.requireBinding */
    protected inline fun <reified T> requireBinding() {
        requireBinding(T::class.java)
    }

    /** @see KotlinBinder.getProvider */
    protected inline fun <reified T> getProvider(): Provider<T> {
        return kotlinBinder.getProvider<T>()
    }

    /** @see KotlinBinder.getMembersInjector */
    protected inline fun <reified T> getMembersInjector(): MembersInjector<T> {
        return kotlinBinder.getMembersInjector<T>()
    }
}
