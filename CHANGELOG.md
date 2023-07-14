# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Git:
  - _open_repository():
    - Implement _repo.config_writer().set_value("blame", "markUnblamableLines", "true").release()
  - get_list_commits():
    - kwargs full implementation
  - get_commits_last_modified_lines
  - _calculate_last_commits
  - _get_blame
  - _useless_line
  - get_commits_modified_file:
    - check includeDeletedFiles behaviour
- Repository:
  - _prepRepo():
    - implement clear method
  - traverseCommits():
    - Build the arguments to pass to git rev-list

### Changed

- domain/ModifiedFile:
  - Review histogram implementation.
- Repository:
  - traverseCommits():
    - Make it parallel
- utils/Config:
  - Generic Exception to BadName exception
  - build_args(self)
- Test:
  - TestRepository:
    - testIgnoreDeletedWhitespaces
    - testIgnoreAddWhitespacesAndChangedFile

## [0.5.0] - 2023-07-14

### Added

- Support to Kotlin Multiplatform (KMP).

## [0.4.0] - 2023-07-14 [YANKED]

### Added

- Git
  - checkout(self, _hash: str) -> None
  - reset(self) -> None
- Test:
  - TestGit

### Changed

- Git
  - Review reverse behaviour with PyDriller

## [0.3.0] - 2023-07-07

### Added

- domain/ModifiedFile:
  - histogram
  - skip_whitespaces
- Git:
  - Add list of revFilter.
  - Implement reverse if in kwargs.
- Repository:
  - TextProgressMonitor when cloneRepository.
  - Pass config to Git class.
  - Pass revFilter to getListCommits.
- utils/Config:
  - Partial implementation of build_args(self).
- test:
  - logback.xml config file.
  - Beta version of TestRepository.

### Fixed

- Repository:
  - clone_repo_to option didn't work.

## [0.2.0] - 2023-07-05 

### Added 

- Repository:
  - _prepRepo():
    - Make _conf.sanityCheckFilters() available.

## [0.1.0] - 2023-07-04 

### Added 

- Utils:
  - Partial Conf implementation.
- Domain:
  - Partial Commit implementation.
  - Complete Developer implementation.
- Partial Git implementation.
- Partial Repository implementation.
- Apache 2.0 license based on PyDriller repo.
- Tests:
  - Complete TestDeveloper implementation.
  - Partial TestRepository implementation.
  - Partial TestGit implementation.
  - test-repos.zip to test resources folder.
- Documentation:
  - Doc strings to Git and Repository classes
