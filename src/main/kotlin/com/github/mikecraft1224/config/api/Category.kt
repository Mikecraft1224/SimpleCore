package com.github.mikecraft1224.config.api

/**
 * Annotates a field inside a Config as a category.
 * When used inside a [Category] it will be a subcategory.
 * Subcategories cannot be nested further.
 *
 * This may only be used on non-primitive custom objects.
 *
 * @param name The name of the category.
 * @param description The description of the category, shown at the top of the category page.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Category(
    val name: String,
    val description: String = "",
)
