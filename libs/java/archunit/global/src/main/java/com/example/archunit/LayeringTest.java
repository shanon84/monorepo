package com.example.archunit;

import com.example.global.annotations.Persistence;
import com.example.global.annotations.Properties;
import com.example.global.annotations.Scheduler;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(packages = "*", importOptions = {ImportOption.DoNotIncludeArchives.class, ImportOption.DoNotIncludeJars.class, ImportOption.DoNotIncludeTests.class})
public class LayeringTest {

  public static final String CONTROLLERS = "Controllers";
  public static final String SCHEDULERS = "Schedulers";
  public static final String CONFIGURATIONS = "Configurations";
  public static final String PROPERTIES = "Properties";
  public static final String SERVICES = "Services";
  public static final String PERSISTENCES = "Persistences";
  public static final String REPOSITORIES = "Repositories";

  @ArchTest
  static final ArchRule layer_dependencies_are_respected = Architectures.layeredArchitecture().consideringAllDependencies()

    .optionalLayer(CONFIGURATIONS).definedBy(CanBeAnnotated.Predicates.annotatedWith(Configuration.class))
    .optionalLayer(CONTROLLERS).definedBy(CanBeAnnotated.Predicates.annotatedWith(Controller.class).or(CanBeAnnotated.Predicates.annotatedWith(RestController.class)))
    .optionalLayer(SCHEDULERS).definedBy(CanBeAnnotated.Predicates.annotatedWith(Scheduler.class))
    .optionalLayer(PROPERTIES).definedBy(CanBeAnnotated.Predicates.annotatedWith(Properties.class))
    .optionalLayer(SERVICES).definedBy(CanBeAnnotated.Predicates.annotatedWith(Service.class))
    .optionalLayer(PERSISTENCES).definedBy(CanBeAnnotated.Predicates.annotatedWith(Persistence.class))
    .optionalLayer(REPOSITORIES).definedBy(CanBeAnnotated.Predicates.annotatedWith(Repository.class))

    .whereLayer(CONFIGURATIONS).mayNotBeAccessedByAnyLayer()
    .whereLayer(CONTROLLERS).mayOnlyBeAccessedByLayers(CONFIGURATIONS)
    .whereLayer(SCHEDULERS).mayOnlyBeAccessedByLayers(CONFIGURATIONS)
    .whereLayer(SERVICES).mayOnlyBeAccessedByLayers(CONTROLLERS, SCHEDULERS, CONFIGURATIONS)
    .whereLayer(PERSISTENCES).mayOnlyBeAccessedByLayers(SERVICES)
    .whereLayer(REPOSITORIES).mayOnlyBeAccessedByLayers(PERSISTENCES);
}

