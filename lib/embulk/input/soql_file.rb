Embulk::JavaPlugin.register_input(
  "soql_file", "org.embulk.input.soql.SoqlFilePlugin",
  File.expand_path('../../../../classpath', __FILE__))
