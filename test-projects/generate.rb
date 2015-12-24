#!/usr/bin/env ruby

require 'FileUtils'
include FileUtils

dependency_managers = %w(system bundler)
version_managers = %w(system rvm rbenv)

version_managers.each do |vm|
  dependency_managers.each do |dm|
    name = [vm, dm].join('-')

    rm_rf name
    mkdir_p name

    ln_s '../shared/test.rb', name
    ln_s '../shared/.rubocop.yml', name

    if vm == 'rvm'
      ln_s '../shared/.ruby-version', name
      ln_s '../shared/.ruby-gemset', name
    elsif vm == 'rbenv'
      ln_s '../shared/.ruby-version', name
    end

    if dm == 'bundler'
      ln_s '../shared/Gemfile', name
      ln_s '../shared/Gemfile.lock', name
    end
  end
end
