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
package org.eclipse.che.api.vfs.search.impl;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import com.google.common.base.Optional;
import java.io.File;
import java.util.Collections;
import java.util.List;
import org.eclipse.che.api.vfs.ArchiverFactory;
import org.eclipse.che.api.vfs.VirtualFile;
import org.eclipse.che.api.vfs.VirtualFileFilter;
import org.eclipse.che.api.vfs.VirtualFileSystem;
import org.eclipse.che.api.vfs.impl.memory.MemoryVirtualFileSystem;
import org.eclipse.che.api.vfs.search.QueryExpression;
import org.eclipse.che.api.vfs.search.SearchResult;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.commons.lang.NameGenerator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("Duplicates")
public class FSLuceneSearcherTest {
  private static final String[] TEST_CONTENT = {
    "Apollo set several major human spaceflight milestones",
    "Maybe you should think twice",
    "To be or not to be beeeee lambergeeene",
    "In early 1961, direct ascent was generally the mission mode in favor at NASA",
    "Time to think"
  };

  private File indexDirectory;
  private VirtualFileFilter filter;
  private FSLuceneSearcher searcher;
  private AbstractLuceneSearcherProvider.CloseCallback closeCallback;

  @BeforeMethod
  public void setUp() throws Exception {
    File targetDir =
        new File(Thread.currentThread().getContextClassLoader().getResource(".").getPath())
            .getParentFile();
    indexDirectory = new File(targetDir, NameGenerator.generate("index-", 4));
    assertTrue(indexDirectory.mkdir());

    filter = mock(VirtualFileFilter.class);
    when(filter.accept(any(VirtualFile.class))).thenReturn(false);

    closeCallback = mock(AbstractLuceneSearcherProvider.CloseCallback.class);
    searcher = new FSLuceneSearcher(indexDirectory, filter, closeCallback);
  }

  @AfterMethod
  public void tearDown() throws Exception {
    searcher.close();
    IoUtil.deleteRecursive(indexDirectory);
  }

  @Test
  public void initializesIndexForExistedFiles() throws Exception {
    VirtualFileSystem virtualFileSystem = virtualFileSystem();
    VirtualFile folder = virtualFileSystem.getRoot().createFolder("folder");
    folder.createFile("xxx.txt", TEST_CONTENT[2]);
    folder.createFile("zzz.txt", TEST_CONTENT[1]);
    searcher.init(virtualFileSystem);

    List<String> paths = searcher.search(new QueryExpression().setText("think")).getFilePaths();
    assertEquals(newArrayList("/folder/zzz.txt"), paths);
  }

  @Test
  public void addsSingleFileInIndex() throws Exception {
    VirtualFileSystem virtualFileSystem = virtualFileSystem();
    searcher.init(virtualFileSystem);
    VirtualFile file =
        virtualFileSystem.getRoot().createFolder("aaa").createFile("aaa.txt", TEST_CONTENT[1]);

    searcher.add(file);

    List<String> paths = searcher.search(new QueryExpression().setText("should")).getFilePaths();
    assertEquals(newArrayList(file.getPath().toString()), paths);
  }

  @Test
  public void addsFileTreeInIndex() throws Exception {
    VirtualFileSystem virtualFileSystem = virtualFileSystem();
    searcher.init(virtualFileSystem);
    VirtualFile folder = virtualFileSystem.getRoot().createFolder("folder");
    folder.createFile("xxx.txt", TEST_CONTENT[2]);
    folder.createFile("zzz.txt", TEST_CONTENT[1]);

    searcher.add(virtualFileSystem.getRoot());

    List<String> paths = searcher.search(new QueryExpression().setText("be")).getFilePaths();
    assertEquals(newArrayList("/folder/xxx.txt"), paths);
    paths = searcher.search(new QueryExpression().setText("should")).getFilePaths();
    assertEquals(newArrayList("/folder/zzz.txt"), paths);
  }

  @Test
  public void updatesSingleFileInIndex() throws Exception {
    VirtualFileSystem virtualFileSystem = virtualFileSystem();
    VirtualFile file =
        virtualFileSystem.getRoot().createFolder("aaa").createFile("aaa.txt", TEST_CONTENT[2]);
    searcher.init(virtualFileSystem);

    List<String> paths = searcher.search(new QueryExpression().setText("should")).getFilePaths();
    assertTrue(paths.isEmpty());

    file.updateContent(TEST_CONTENT[1]);
    searcher.update(file);

    paths = searcher.search(new QueryExpression().setText("should")).getFilePaths();

    assertEquals(newArrayList(file.getPath().toString()), paths);
  }

  @Test
  public void deletesSingleFileFromIndex() throws Exception {
    VirtualFileSystem virtualFileSystem = virtualFileSystem();
    VirtualFile file =
        virtualFileSystem.getRoot().createFolder("aaa").createFile("aaa.txt", TEST_CONTENT[2]);
    searcher.init(virtualFileSystem);

    List<String> paths = searcher.search(new QueryExpression().setText("be")).getFilePaths();
    assertEquals(newArrayList(file.getPath().toString()), paths);

    searcher.delete(file.getPath().toString(), file.isFile());

    paths = searcher.search(new QueryExpression().setText("be")).getFilePaths();
    assertTrue(paths.isEmpty());
  }

  @Test
  public void deletesFileTreeFromIndex() throws Exception {
    VirtualFileSystem virtualFileSystem = virtualFileSystem();
    VirtualFile folder = virtualFileSystem.getRoot().createFolder("folder");
    folder.createFile("xxx.txt", TEST_CONTENT[2]);
    folder.createFile("zzz.txt", TEST_CONTENT[1]);
    searcher.init(virtualFileSystem);

    List<String> paths = searcher.search(new QueryExpression().setText("be")).getFilePaths();
    assertEquals(newArrayList("/folder/xxx.txt"), paths);
    paths = searcher.search(new QueryExpression().setText("should")).getFilePaths();
    assertEquals(newArrayList("/folder/zzz.txt"), paths);

    searcher.delete("/folder", false);

    paths = searcher.search(new QueryExpression().setText("be")).getFilePaths();
    assertTrue(paths.isEmpty());
    paths = searcher.search(new QueryExpression().setText("should")).getFilePaths();
    assertTrue(paths.isEmpty());
  }

  @Test
  public void searchesByWordFragment() throws Exception {
    VirtualFileSystem virtualFileSystem = virtualFileSystem();
    VirtualFile folder = virtualFileSystem.getRoot().createFolder("folder");
    folder.createFile("xxx.txt", TEST_CONTENT[0]);
    searcher.init(virtualFileSystem);

    List<String> paths = searcher.search(new QueryExpression().setText("*stone*")).getFilePaths();
    assertEquals(newArrayList("/folder/xxx.txt"), paths);
  }

  @Test
  public void searchesByTextAndFileName() throws Exception {
    VirtualFileSystem virtualFileSystem = virtualFileSystem();
    VirtualFile folder = virtualFileSystem.getRoot().createFolder("folder");
    folder.createFile("xxx.txt", TEST_CONTENT[2]);
    folder.createFile("zzz.txt", TEST_CONTENT[2]);
    searcher.init(virtualFileSystem);

    List<String> paths =
        searcher.search(new QueryExpression().setText("be").setName("xxx.txt")).getFilePaths();
    assertEquals(newArrayList("/folder/xxx.txt"), paths);
  }

  @Test
  public void searchesByFullTextAndFileName() throws Exception {
    VirtualFileSystem virtualFileSystem = virtualFileSystem();
    VirtualFile folder = virtualFileSystem.getRoot().createFolder("folder");
    folder.createFile("xxx.txt", TEST_CONTENT[2]);
    folder.createFile("zzz.txt", TEST_CONTENT[2]);
    searcher.init(virtualFileSystem);

    SearchResult result =
        searcher.search(
            new QueryExpression().setText("*be*").setName("xxx.txt").setIncludePositions(true));
    List<String> paths = result.getFilePaths();
    assertEquals(newArrayList("/folder/xxx.txt"), paths);
    assertEquals(result.getResults().get(0).getData().size(), 4);
  }

  @Test
  public void searchesByFullTextAndFileName2() throws Exception {
    VirtualFileSystem virtualFileSystem = virtualFileSystem();
    VirtualFile folder = virtualFileSystem.getRoot().createFolder("folder");
    folder.createFile("xxx.txt", TEST_CONTENT[2]);
    folder.createFile("zzz.txt", TEST_CONTENT[4]);
    searcher.init(virtualFileSystem);

    SearchResult result =
        searcher.search(new QueryExpression().setText("*to*").setIncludePositions(true));
    List<String> paths = result.getFilePaths();
    assertEquals(paths.size(), 2);
    assertEquals(result.getResults().get(0).getData().size(), 2);
  }

  @DataProvider
  public Object[][] searchByName() {
    return new Object[][] {
      {"sameName.txt", "sameName.txt"},
      {"notCaseSensitive.txt", "notcasesensitive.txt"},
      {"fullName.txt", "full*"},
      {"file name.txt", "file name"},
      {"prefixFileName.txt", "prefixF*"},
      {"name.with.dot.txt", "name.With.Dot.txt"},
    };
  }

  @Test(dataProvider = "searchByName")
  public void searchFileByName(String fileName, String searchedFileName) throws Exception {
    VirtualFileSystem virtualFileSystem = virtualFileSystem();
    VirtualFile folder = virtualFileSystem.getRoot().createFolder("parent/child");
    VirtualFile folder2 = virtualFileSystem.getRoot().createFolder("folder2");
    folder.createFile(NameGenerator.generate(null, 10), TEST_CONTENT[3]);
    folder.createFile(fileName, TEST_CONTENT[2]);
    folder.createFile(NameGenerator.generate(null, 10), TEST_CONTENT[1]);
    folder2.createFile(NameGenerator.generate(null, 10), TEST_CONTENT[2]);
    folder2.createFile(NameGenerator.generate(null, 10), TEST_CONTENT[2]);
    searcher.init(virtualFileSystem);

    List<String> paths =
        searcher.search(new QueryExpression().setName(searchedFileName)).getFilePaths();
    assertEquals(newArrayList("/parent/child/" + fileName), paths);
  }

  @Test
  public void searchesByTextAndPath() throws Exception {
    VirtualFileSystem virtualFileSystem = virtualFileSystem();
    VirtualFile folder1 = virtualFileSystem.getRoot().createFolder("folder1/a/b");
    VirtualFile folder2 = virtualFileSystem.getRoot().createFolder("folder2");
    folder1.createFile("xxx.txt", TEST_CONTENT[2]);
    folder2.createFile("zzz.txt", TEST_CONTENT[2]);
    searcher.init(virtualFileSystem);

    List<String> paths =
        searcher.search(new QueryExpression().setText("be").setPath("/folder1")).getFilePaths();
    assertEquals(newArrayList("/folder1/a/b/xxx.txt"), paths);
  }

  @Test
  public void searchesByTextAndPathAndFileName() throws Exception {
    VirtualFileSystem virtualFileSystem = virtualFileSystem();
    VirtualFile folder1 = virtualFileSystem.getRoot().createFolder("folder1/a/b");
    VirtualFile folder2 = virtualFileSystem.getRoot().createFolder("folder2/a/b");
    folder1.createFile("xxx.txt", TEST_CONTENT[2]);
    folder1.createFile("yyy.txt", TEST_CONTENT[2]);
    folder2.createFile("zzz.txt", TEST_CONTENT[2]);
    searcher.init(virtualFileSystem);

    List<String> paths =
        searcher
            .search(new QueryExpression().setText("be").setPath("/folder1").setName("xxx.txt"))
            .getFilePaths();
    assertEquals(newArrayList("/folder1/a/b/xxx.txt"), paths);
  }

  @Test
  public void closesLuceneIndexWriterWhenSearcherClosed() throws Exception {
    VirtualFileSystem virtualFileSystem = virtualFileSystem();
    searcher.init(virtualFileSystem);

    searcher.close();

    assertTrue(searcher.isClosed());
    assertFalse(searcher.getIndexWriter().isOpen());
  }

  @Test
  public void notifiesCallbackWhenSearcherClosed() throws Exception {
    VirtualFileSystem virtualFileSystem = virtualFileSystem();
    searcher.init(virtualFileSystem);

    searcher.close();
    verify(closeCallback).onClose();
  }

  @Test
  public void excludesFilesFromIndexWithFilter() throws Exception {
    VirtualFileSystem virtualFileSystem = virtualFileSystem();
    VirtualFile folder = virtualFileSystem.getRoot().createFolder("folder");
    folder.createFile("xxx.txt", TEST_CONTENT[2]);
    folder.createFile("yyy.txt", TEST_CONTENT[2]);
    folder.createFile("zzz.txt", TEST_CONTENT[2]);

    when(filter.accept(withName("yyy.txt"))).thenReturn(true);
    searcher.init(virtualFileSystem);

    List<String> paths = searcher.search(new QueryExpression().setText("be")).getFilePaths();
    assertEquals(newArrayList("/folder/xxx.txt", "/folder/zzz.txt"), paths);
  }

  @Test
  public void limitsNumberOfSearchResultsWhenMaxItemIsSet() throws Exception {
    VirtualFileSystem virtualFileSystem = virtualFileSystem();
    for (int i = 0; i < 125; i++) {
      virtualFileSystem
          .getRoot()
          .createFile(String.format("file%02d", i), TEST_CONTENT[i % TEST_CONTENT.length]);
    }
    searcher.init(virtualFileSystem);

    SearchResult result = searcher.search(new QueryExpression().setText("mission").setMaxItems(5));

    assertEquals(25, result.getTotalHits());
    assertEquals(5, result.getFilePaths().size());
  }

  @Test
  public void generatesQueryExpressionForRetrievingNextPageOfResults() throws Exception {
    VirtualFileSystem virtualFileSystem = virtualFileSystem();
    for (int i = 0; i < 125; i++) {
      virtualFileSystem
          .getRoot()
          .createFile(String.format("file%02d", i), TEST_CONTENT[i % TEST_CONTENT.length]);
    }
    searcher.init(virtualFileSystem);

    SearchResult result =
        searcher.search(new QueryExpression().setText("spaceflight").setMaxItems(7));

    assertEquals(result.getTotalHits(), 25);

    Optional<QueryExpression> optionalNextPageQueryExpression = result.getNextPageQueryExpression();
    assertTrue(optionalNextPageQueryExpression.isPresent());
    QueryExpression nextPageQueryExpression = optionalNextPageQueryExpression.get();
    assertEquals("spaceflight", nextPageQueryExpression.getText());
    assertEquals(7, nextPageQueryExpression.getSkipCount());
    assertEquals(7, nextPageQueryExpression.getMaxItems());
  }

  @Test
  public void retrievesSearchResultWithPages() throws Exception {
    VirtualFileSystem virtualFileSystem = virtualFileSystem();
    for (int i = 0; i < 125; i++) {
      virtualFileSystem
          .getRoot()
          .createFile(String.format("file%02d", i), TEST_CONTENT[i % TEST_CONTENT.length]);
    }
    searcher.init(virtualFileSystem);

    SearchResult firstPage =
        searcher.search(new QueryExpression().setText("spaceflight").setMaxItems(8));
    assertEquals(firstPage.getFilePaths().size(), 8);

    QueryExpression nextPageQueryExpression = firstPage.getNextPageQueryExpression().get();
    nextPageQueryExpression.setMaxItems(100);

    SearchResult lastPage = searcher.search(nextPageQueryExpression);
    assertEquals(lastPage.getFilePaths().size(), 17);

    assertTrue(Collections.disjoint(firstPage.getFilePaths(), lastPage.getFilePaths()));
  }

  private VirtualFileSystem virtualFileSystem() throws Exception {
    return new MemoryVirtualFileSystem(mock(ArchiverFactory.class), null);
  }

  private static VirtualFile withName(String name) {
    return argThat(argument -> name.equals((argument).getName()));
  }
}
