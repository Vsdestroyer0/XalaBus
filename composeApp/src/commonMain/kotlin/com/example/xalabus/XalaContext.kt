package com.example.xalabus

import com.example.xalabus.DBD.AppDatabase
import com.example.xalabus.data.repository.RouteRepository
import com.example.xalabus.db.DriverFactory

object XalaContext {
    private var repository: RouteRepository? = null

    fun getRepository(driverFactory: DriverFactory): RouteRepository {
        if (repository == null) {
            val driver = driverFactory.createDriver()
            val database = AppDatabase(driver)
            repository = RouteRepository(database)
        }
        return repository!!
    }
}