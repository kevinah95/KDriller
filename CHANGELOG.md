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
    - Implement reverse if in kwargs
  - checkout(self, _hash: str) -> None
  - reset(self) -> None
  - get_commits_last_modified_lines
  - _calculate_last_commits
  - _get_blame
  - _useless_line
  - get_commits_modified_file:
    - implement includeDeletedFiles
- Repository:
  - _prepRepo():
    - implement clear method
  - traverseCommits():
    - Build the arguments to pass to git rev-list
- utils/Config:
  - build_args(self)

### Changed

- Repository:
  - traverseCommits():
    - Make it parallel
- utils/Config:
  - Generic Exception to BadName exception

## [0.1.0] - 2023-07-05 

### Added 

- Repository:
  - _prepRepo():
    - Make _conf.sanityCheckFilters() available

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
