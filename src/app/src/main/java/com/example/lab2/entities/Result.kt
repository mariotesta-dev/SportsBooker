package com.example.lab2.entities

class Result<out T> (
    val value: T?,
    val throwable: Throwable?
)