Embulk::JavaPlugin.register_input(
  "soql", "org.embulk.input.soql.SoqlFilePlugin",
  File.expand_path('../../../../classpath', __FILE__))
