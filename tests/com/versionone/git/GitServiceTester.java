package com.versionone.git;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class GitServiceTester {

    private final String configurationPathMessages = GitServiceTester.class.getResource("test_configuration.xml").getPath();
    private final String configurationPathBranchNames = GitServiceTester.class.getResource("test_configuration_branches.xml").getPath();

    private JUnit4Mockery context;
    private IGitConnector gitConnectorMock;
    private IDbStorage storageMock;
    private IChangeSetWriter v1ConnectorMock;

    private Configuration getConfigurationWithBranchProcessingEnabled() {
        return Configuration.getInstance(configurationPathBranchNames);
    }

    private Configuration getConfigurationWithBranchProcessingDisabled() {
        return Configuration.getInstance(configurationPathMessages);
    }

    @Before
    public void before() {
        context = new JUnit4Mockery();
        gitConnectorMock = context.mock(IGitConnector.class);
        storageMock = context.mock(IDbStorage.class);
        v1ConnectorMock = context.mock(IChangeSetWriter.class);
        Configuration.reset();
    }

    @Test
    public void emptyChangesetTest() throws GitException, VersionOneException {
        final Configuration config = getConfigurationWithBranchProcessingDisabled();

        GitService service = new GitService(config, storageMock, gitConnectorMock, v1ConnectorMock);

        context.checking(new Expectations() {{
            oneOf(gitConnectorMock).cleanupLocalDirectory();
            oneOf(gitConnectorMock).initRepository();
            oneOf(v1ConnectorMock).connect();
            oneOf(gitConnectorMock).getBranchCommits(); will(returnValue(new LinkedList()));
        }});

        service.initialize();
        service.onInterval();
    }

    @Test
    public void branchCommitsTest() throws GitException, VersionOneException {
        final Configuration config = getConfigurationWithBranchProcessingDisabled();

        GitService service = new GitService(config, storageMock, gitConnectorMock, v1ConnectorMock);

        final ChangeSetInfo firstChange = new ChangeSetInfo("user", "first commit", "1", new Date());
        final ChangeSetInfo secondChange = new ChangeSetInfo("user", "second commit", "2", new Date());
        final List<ChangeSetInfo> changes = new LinkedList<ChangeSetInfo>();
        changes.add(firstChange);
        changes.add(secondChange);

        context.checking(new Expectations() {{
            oneOf(gitConnectorMock).cleanupLocalDirectory();
            oneOf(gitConnectorMock).initRepository();
            oneOf(v1ConnectorMock).connect();
            oneOf(gitConnectorMock).getBranchCommits();
                will(returnValue(changes));
            PersistentChange firstPersistentChange = PersistentChange.createNew(firstChange.getRevision());
            PersistentChange secondPersistentChange = PersistentChange.createNew(secondChange.getRevision());
            oneOf(storageMock).isChangePersisted(firstPersistentChange);
                will(returnValue(false));
            oneOf(v1ConnectorMock).publish(firstChange);
            oneOf(storageMock).persistChange(firstPersistentChange);
            oneOf(storageMock).isChangePersisted(secondPersistentChange);
                will(returnValue(true));
            never(v1ConnectorMock).publish(secondChange);
            never(storageMock).persistChange(secondPersistentChange);
        }});

        service.initialize();
        service.onInterval();
    }

    @Test
    public void branchNamesTest() throws GitException, VersionOneException {
        final Configuration config = getConfigurationWithBranchProcessingEnabled();

        GitService service = new GitService(config, storageMock, gitConnectorMock, v1ConnectorMock);

        final ChangeSetInfo firstChange = new ChangeSetInfo("user", "first commit", "1", new Date());
        final ChangeSetInfo secondChange = new ChangeSetInfo("user", "second commit", "2", new Date());
        final List<ChangeSetInfo> changes = new LinkedList<ChangeSetInfo>();
        changes.add(firstChange);
        changes.add(secondChange);

        context.checking(new Expectations() {{
            oneOf(gitConnectorMock).cleanupLocalDirectory();
            oneOf(gitConnectorMock).initRepository();
            oneOf(v1ConnectorMock).connect();
            oneOf(gitConnectorMock).getMergedBranches();
                will(returnValue(changes));
            PersistentChange firstPersistentChange = PersistentChange.createNew(firstChange.getRevision());
            PersistentChange secondPersistentChange = PersistentChange.createNew(secondChange.getRevision());
            oneOf(storageMock).isChangePersisted(firstPersistentChange);
                will(returnValue(false));
            oneOf(v1ConnectorMock).publish(firstChange);
            oneOf(storageMock).persistChange(firstPersistentChange);
            oneOf(storageMock).isChangePersisted(secondPersistentChange);
                will(returnValue(false));
            oneOf(v1ConnectorMock).publish(secondChange);
            oneOf(storageMock).persistChange(secondPersistentChange);
        }});

        service.initialize();
        service.onInterval();
    }
}