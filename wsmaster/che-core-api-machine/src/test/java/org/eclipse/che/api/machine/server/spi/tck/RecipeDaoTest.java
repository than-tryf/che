/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.machine.server.spi.tck;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.inject.Inject;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.machine.server.event.BeforeRecipeRemovedEvent;
import org.eclipse.che.api.machine.server.event.RecipePersistedEvent;
import org.eclipse.che.api.machine.server.recipe.RecipeImpl;
import org.eclipse.che.api.machine.server.spi.RecipeDao;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.commons.test.tck.TckListener;
import org.eclipse.che.commons.test.tck.repository.TckRepository;
import org.eclipse.che.core.db.cascade.CascadeEventSubscriber;
import org.eclipse.che.core.db.cascade.event.CascadeEvent;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Tests {@link RecipeDao} contract.
 *
 * @author Anton Korneta
 */
@Listeners(TckListener.class)
@Test(suiteName = RecipeDaoTest.SUITE_NAME)
public class RecipeDaoTest {

  public static final String SUITE_NAME = "RecipeDaoTck";

  private static final int ENTRY_COUNT = 5;

  private List<RecipeImpl> recipes;

  @Inject private RecipeDao recipeDao;

  @Inject private TckRepository<RecipeImpl> tckRepository;

  @Inject private EventService eventService;

  @BeforeMethod
  public void setUp() throws Exception {
    recipes = new ArrayList<>(5);
    for (int i = 0; i < ENTRY_COUNT; i++) {
      recipes.add(createRecipe(i));
    }
    tckRepository.createAll(recipes);
  }

  @AfterMethod
  public void cleanUp() throws Exception {
    tckRepository.removeAll();
  }

  @Test(dependsOnMethods = "shouldGetRecipeById")
  public void shouldCreateRecipe() throws Exception {
    final RecipeImpl recipe = createRecipe(0);
    recipeDao.create(recipe);

    assertEquals(recipeDao.getById(recipe.getId()), recipe);
  }

  @Test(
    dependsOnMethods = "shouldThrowNotFoundExceptionWhenGettingNonExistingRecipe",
    expectedExceptions = NotFoundException.class
  )
  public void shouldNotCreateRecipeWhenSubscriberThrowsExceptionOnRecipeStoring() throws Exception {
    final RecipeImpl recipe = createRecipe(0);

    CascadeEventSubscriber<RecipePersistedEvent> subscriber = mockCascadeEventSubscriber();
    doThrow(new ConflictException("error")).when(subscriber).onCascadeEvent(any());
    eventService.subscribe(subscriber, RecipePersistedEvent.class);

    try {
      recipeDao.create(recipe);
      fail("RecipeDao#create had to throw conflict exception");
    } catch (ConflictException ignored) {
    }

    eventService.unsubscribe(subscriber, RecipePersistedEvent.class);
    recipeDao.getById(recipe.getId());
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void shouldThrowNpeWhenCreateNullRecipe() throws Exception {
    recipeDao.create(null);
  }

  @Test(expectedExceptions = ConflictException.class)
  public void shouldThrowConflictExceptionWhenCreatingRecipeWithExistingId() throws Exception {
    recipeDao.create(recipes.get(0));
  }

  @Test
  public void shouldUpdateRecipe() throws Exception {
    final RecipeImpl update = recipes.get(0).withName("updatedName");

    assertEquals(recipeDao.update(update), update);
  }

  @Test
  public void shouldUpdateRecipeWithAllRelatedAttributes() throws Exception {
    final RecipeImpl update = recipes.get(0);
    update
        .withName("debian")
        .withCreator("userid_9")
        .withDescription("description")
        .withType("docker")
        .setScript("FROM codenvy/debian_jdk8");
    recipeDao.update(update);

    assertEquals(recipeDao.getById(update.getId()), update);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void shouldThrowNpeWhenUpdatingRecipeNull() throws Exception {
    recipeDao.update(null);
  }

  @Test(expectedExceptions = NotFoundException.class)
  public void shouldThrowNotFoundExceptionWhenUpdatingNonExistingRecipe() throws Exception {
    recipeDao.update(createRecipe(7));
  }

  @Test(
    expectedExceptions = NotFoundException.class,
    dependsOnMethods = "shouldThrowNotFoundExceptionWhenGettingNonExistingRecipe"
  )
  public void shouldRemoveRecipe() throws Exception {
    final String existedId = recipes.get(0).getId();

    recipeDao.remove(existedId);
    recipeDao.getById(existedId);
  }

  @Test(dependsOnMethods = "shouldGetRecipeById")
  public void shouldNotRemoveRecipeWhenSubscriberThrowsExceptionOnRecipeRemoving()
      throws Exception {
    final RecipeImpl recipe = recipes.get(0);
    CascadeEventSubscriber<BeforeRecipeRemovedEvent> subscriber = mockCascadeEventSubscriber();
    doThrow(new ServerException("error")).when(subscriber).onCascadeEvent(any());
    eventService.subscribe(subscriber, BeforeRecipeRemovedEvent.class);

    try {
      recipeDao.remove(recipe.getId());
      fail("RecipeDao#remove had to throw server exception");
    } catch (ServerException ignored) {
    }

    assertEquals(recipeDao.getById(recipe.getId()), recipe);
    eventService.unsubscribe(subscriber, BeforeRecipeRemovedEvent.class);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void shouldThrowNpeWhenRemovingRecipeIdNull() throws Exception {
    recipeDao.remove(null);
  }

  @Test
  public void shouldGetRecipeById() throws Exception {
    final RecipeImpl recipe = recipes.get(0);

    assertEquals(recipeDao.getById(recipe.getId()), recipe);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void shouldThrowNpeWhenGettingRecipeIdNull() throws Exception {
    recipeDao.getById(null);
  }

  @Test(expectedExceptions = NotFoundException.class)
  public void shouldThrowNotFoundExceptionWhenGettingNonExistingRecipe() throws Exception {
    recipeDao.getById("non-existing");
  }

  @Test
  public void shouldFindRecipeByUser() throws Exception {
    final List<RecipeImpl> result = recipeDao.search(null, null, null, 0, recipes.size());

    assertTrue(result.contains(recipes.get(0)));
  }

  @Test(dependsOnMethods = "shouldFindRecipeByUser")
  public void shouldFindingRecipesByTags() throws Exception {
    final List<String> tags = ImmutableList.of("search-by1", "search-by2");
    recipes.get(0).getTags().addAll(tags);
    recipes.get(1).getTags().add(tags.get(0));
    recipes.get(2).getTags().add(tags.get(1));
    recipes.get(4).getTags().clear();
    updateAll();

    final List<RecipeImpl> result = recipeDao.search(null, tags, null, 0, recipes.size());

    assertEquals(new HashSet<>(result), ImmutableSet.of(recipes.get(0)));
  }

  @Test(dependsOnMethods = "shouldFindRecipeByUser")
  public void shouldFindRecipeByType() throws Exception {
    final RecipeImpl recipe = recipes.get(0);
    final List<RecipeImpl> result =
        recipeDao.search(null, null, recipe.getType(), 0, recipes.size());

    assertTrue(result.contains(recipe));
  }

  @Test(
    dependsOnMethods = {
      "shouldFindRecipeByUser",
      "shouldFindingRecipesByTags",
      "shouldFindRecipeByType"
    }
  )
  public void shouldFindRecipeByUserTagsAndType() throws Exception {
    final RecipeImpl recipe = recipes.get(0);
    final List<RecipeImpl> result =
        recipeDao.search(null, recipe.getTags(), recipe.getType(), 0, 1);

    assertTrue(result.contains(recipe));
  }

  private static RecipeImpl createRecipe(int index) {
    final String recipeId = NameGenerator.generate("recipeId", 5);
    return new RecipeImpl(
        recipeId,
        "recipeName" + index,
        "creator" + index,
        "dockerfile" + index,
        "script",
        new ArrayList<>(asList("tag1" + index, "tag2" + index)),
        "recipe description");
  }

  private void updateAll() throws Exception {
    for (RecipeImpl recipe : recipes) {
      recipeDao.update(recipe);
    }
  }

  private <T extends CascadeEvent> CascadeEventSubscriber<T> mockCascadeEventSubscriber() {
    @SuppressWarnings("unchecked")
    CascadeEventSubscriber<T> subscriber = mock(CascadeEventSubscriber.class);
    doCallRealMethod().when(subscriber).onEvent(any());
    return subscriber;
  }
}
