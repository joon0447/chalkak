package com.joon.chalkak.data.location

class LocationProviderUnavailableException : IllegalStateException(
    "No enabled location provider is available."
)
